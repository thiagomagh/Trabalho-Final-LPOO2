package sistemapizzaria.controller;

import java.util.Collections;
import java.util.List;

import sistemapizzaria.cliente.Cliente;
import sistemapizzaria.cliente.Pedido;
import sistemapizzaria.cliente.StatusPedido;
import sistemapizzaria.dao.ClienteDao;
import sistemapizzaria.dao.DaoFactory;
import sistemapizzaria.dao.PedidoDao;
import sistemapizzaria.dao.SaborDao;
import sistemapizzaria.excecoes.PedidoNaoEditavelException;
import sistemapizzaria.model.CarrinhoPizzaTableModel;
import sistemapizzaria.model.ClienteTableModel;
import sistemapizzaria.model.PedidoTableModel;
import sistemapizzaria.pizza.Pizza;
import sistemapizzaria.pizza.Sabor;
import sistemapizzaria.telas.TelaPedidos;

/**
 * Controlador responsável pela realização e edição de Pedidos de Pizzas (Padrão MVC - Aula 06).
 */
public class PedidoController {

    private final TelaPedidos view;
    private final ClienteTableModel modeloClientes;
    private final PedidoTableModel modeloPedidos;
    private final CarrinhoPizzaTableModel modeloCarrinho;

    private final ClienteDao clienteDao;
    private final PedidoDao pedidoDao;
    private final SaborDao saborDao;

    private Cliente clienteAtual;
    private Pedido pedidoEmEdicao;
    private Pedido pedidoEmAndamento;

    // Posição, no carrinho, da pizza que está sendo editada no painel de montagem (null = nenhuma).
    // A pizza continua no carrinho enquanto é editada: ela só é substituída quando o usuário confirma a alteração.
    private Integer indicePizzaEmEdicao;

    public PedidoController(
            TelaPedidos view,
            ClienteTableModel modeloClientes,
            PedidoTableModel modeloPedidos,
            CarrinhoPizzaTableModel modeloCarrinho) {
        this.view = view;
        this.modeloClientes = modeloClientes;
        this.modeloPedidos = modeloPedidos;
        this.modeloCarrinho = modeloCarrinho;
        this.clienteDao = DaoFactory.getClienteDao();
        this.pedidoDao = DaoFactory.getPedidoDao();
        this.saborDao = DaoFactory.getSaborDao();
    }

    public List<Sabor> listarSaboresDisponiveis() {
        try {
            return saborDao.getAll();
        } catch (Exception e) {
            view.exibirAviso("Erro ao carregar os sabores do banco de dados: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void buscarClientePorTelefone(String telefone) {
        try {
            String tel = (telefone != null) ? telefone.trim() : "";
            if (tel.isEmpty()) {
                modeloClientes.setListaClientes(Collections.emptyList());
                modeloPedidos.setListaPedidos(Collections.emptyList());
                clienteAtual = null;
                return;
            }
            Cliente c = clienteDao.getByTelefone(tel);
            if (c != null) {
                modeloClientes.setListaClientes(Collections.singletonList(c));
                view.selecionarPrimeiraLinhaCliente();
            } else {
                // Tenta busca parcial por telefone caso não encontre exato
                List<Cliente> parciais = clienteDao.buscarPorSobrenomeETelefone("", tel);
                if (parciais.isEmpty()) {
                    modeloClientes.setListaClientes(Collections.emptyList());
                    modeloPedidos.setListaPedidos(Collections.emptyList());
                    clienteAtual = null;
                    ocultarPainel();
                    view.exibirAviso("Cliente não encontrado para o telefone informado.");
                } else if (parciais.size() == 1) {
                    modeloClientes.setListaClientes(parciais);
                    view.selecionarPrimeiraLinhaCliente();
                } else {
                    // Vários clientes combinam com o trecho digitado: quem escolhe é o usuário, na tabela.
                    // Assim o pedido não é aberto, sem querer, para a pessoa errada.
                    modeloClientes.setListaClientes(parciais);
                    modeloPedidos.setListaPedidos(Collections.emptyList());
                    clienteAtual = null;
                    ocultarPainel();
                }
            }
        } catch (Exception e) {
            view.exibirAviso("Erro ao buscar cliente: " + e.getMessage());
        }
    }

    public void aoSelecionarClienteNaTabela(int linhaCliente) {
        if (linhaCliente == -1) {
            clienteAtual = null;
            modeloPedidos.setListaPedidos(Collections.emptyList());
            ocultarPainel();
            return;
        }
        try {
            Cliente selecionado = modeloClientes.getCliente(linhaCliente);
            if (selecionado == null) return;

            // Recarrega cliente e seus pedidos do banco de dados
            this.clienteAtual = clienteDao.getById(selecionado.getId());
            if (this.clienteAtual == null) {
                this.clienteAtual = selecionado;
            }
            List<Pedido> pedidos = this.clienteAtual.getPedidos();
            modeloPedidos.setListaPedidos(pedidos);

            // Requisito 2.b: Ao escolher o cliente, se já possuir pedido aberto, lista os itens para atualização;
            // se não possuir pedido aberto, permite adicionar itens a um novo pedido.
            Pedido aberto = this.clienteAtual.obterPedidoAberto();
            if (aberto != null) {
                int idxPedidoAberto = -1;
                for (int i = 0; i < pedidos.size(); i++) {
                    if (pedidos.get(i).getIdentificador() == aberto.getIdentificador()) {
                        idxPedidoAberto = i;
                        break;
                    }
                }
                if (idxPedidoAberto != -1) {
                    view.selecionarLinhaPedido(idxPedidoAberto);
                    carregarPedidoParaEdicao(aberto);
                }
            } else {
                iniciarNovoPedido();
            }
        } catch (Exception e) {
            view.exibirAviso("Erro ao carregar pedidos do cliente: " + e.getMessage());
        }
    }

    public void iniciarNovoPedido() {
        if (clienteAtual == null) {
            view.exibirAviso("Selecione um cliente primeiro.");
            return;
        }
        this.indicePizzaEmEdicao = null;
        this.pedidoEmEdicao = null;
        this.pedidoEmAndamento = new Pedido();
        atualizarCarrinhoNaView();
        view.exibirPainelMontagemECarrinho(false, null);
    }

    public void editarPedidoSelecionado(int linhaPedido) {
        if (clienteAtual == null || linhaPedido == -1) return;
        Pedido p = modeloPedidos.getPedido(linhaPedido);
        if (p == null) return;

        if (p.getStatus() != StatusPedido.ABERTO) {
            view.exibirAviso("Não é possível alterar um pedido que já está a caminho ou foi entregue.");
            return;
        }
        carregarPedidoParaEdicao(p);
    }

    private void carregarPedidoParaEdicao(Pedido p) {
        this.indicePizzaEmEdicao = null;
        this.pedidoEmEdicao = p;
        this.pedidoEmAndamento = new Pedido(p.getIdentificador(), p.getPrecoTotal(), p.getStatus());
        for (Pizza pizza : p.getPizzas()) {
            this.pedidoEmAndamento.adicionarPizza(pizza);
        }
        atualizarCarrinhoNaView();
        Pizza primeiraPizza = p.getPizzas().isEmpty() ? null : p.getPizzas().get(0);
        view.exibirPainelMontagemECarrinho(true, primeiraPizza);
    }

    public void excluirPedidoSelecionado(int linhaPedido) {
        if (clienteAtual == null || linhaPedido == -1) return;
        Pedido p = modeloPedidos.getPedido(linhaPedido);
        if (p == null) return;

        if (p.getStatus() != StatusPedido.ABERTO) {
            view.exibirAviso("Não é possível excluir um pedido que já está a caminho ou foi entregue.");
            return;
        }

        try {
            // O estado mostrado na tabela pode estar desatualizado (ex.: o pedido foi marcado como entregue em
            // "Gerenciar Pedidos"), então o estado real é conferido no banco antes de excluir.
            Pedido noBanco = pedidoDao.getById(p.getIdentificador());
            if (noBanco == null || noBanco.getStatus() != StatusPedido.ABERTO) {
                recarregarPedidosDoClienteAtual();
                view.exibirAviso(noBanco == null
                        ? "O pedido #" + p.getIdentificador() + " não existe mais."
                        : "Não é possível excluir um pedido que já está a caminho ou foi entregue.");
                return;
            }

            if (!view.confirmarAcao("Deseja realmente excluir o pedido #" + p.getIdentificador() + "?")) {
                return;
            }

            pedidoDao.delete(p.getIdentificador());
            if (pedidoEmEdicao != null && pedidoEmEdicao.getIdentificador() == p.getIdentificador()) {
                pedidoEmEdicao = null;
                pedidoEmAndamento = null;
                ocultarPainel();
            }
            recarregarPedidosDoClienteAtual();
            view.exibirSucesso("Pedido excluído com sucesso!");
        } catch (Exception e) {
            view.exibirAviso("Erro ao excluir pedido do banco de dados: " + e.getMessage());
        }
    }

    public void adicionarPizzaAoCarrinho(Pizza novaPizza) {
        if (pedidoEmAndamento == null) {
            pedidoEmAndamento = new Pedido();
        }
        if (indicePizzaEmEdicao != null) {
            // Estava editando uma pizza do carrinho: a pizza alterada ocupa o lugar da original
            List<Pizza> pizzas = pedidoEmAndamento.getPizzas();
            if (indicePizzaEmEdicao >= 0 && indicePizzaEmEdicao < pizzas.size()) {
                pizzas.set(indicePizzaEmEdicao, novaPizza);
                pedidoEmAndamento.setPizzas(pizzas);
            } else {
                pedidoEmAndamento.adicionarPizza(novaPizza);
            }
            indicePizzaEmEdicao = null;
        } else {
            pedidoEmAndamento.adicionarPizza(novaPizza);
        }
        atualizarCarrinhoNaView();
    }

    public void editarPizzaDoCarrinho(int linhaCarrinho) {
        if (linhaCarrinho == -1) {
            view.exibirAviso("Selecione uma pizza no carrinho para editar.");
            return;
        }
        if (pedidoEmAndamento != null) {
            Pizza pizza = modeloCarrinho.getPizza(linhaCarrinho);
            if (pizza != null) {
                // A pizza continua no carrinho: se o usuário desistir da edição nada se perde
                indicePizzaEmEdicao = linhaCarrinho;
                view.carregarPizzaNoPainelMontagem(pizza);
            }
        }
    }

    // O usuário desistiu de editar a pizza: ela segue no carrinho como estava
    public void cancelarEdicaoPizza() {
        indicePizzaEmEdicao = null;
        view.limparPainelMontagem();
    }

    public void removerPizzaDoCarrinho(int linhaCarrinho) {
        if (linhaCarrinho == -1) {
            view.exibirAviso("Selecione uma pizza no carrinho para remover.");
            return;
        }
        if (pedidoEmAndamento != null) {
            Pizza pizza = modeloCarrinho.getPizza(linhaCarrinho);
            if (pizza != null) {
                pedidoEmAndamento.removerPizza(pizza);
                atualizarCarrinhoNaView();
                // As posições do carrinho mudaram: qualquer edição de pizza em andamento é cancelada
                if (indicePizzaEmEdicao != null) {
                    cancelarEdicaoPizza();
                }
            }
        }
    }

    public void finalizarOuSalvarPedido() {
        if (clienteAtual == null) {
            view.exibirAviso("Selecione um cliente primeiro.");
            return;
        }
        if (indicePizzaEmEdicao != null) {
            view.exibirAviso("Há uma pizza em edição. Clique em \"Atualizar pizza\" para aplicar a alteração "
                    + "ou em \"Cancelar edição\" para manter a pizza original.");
            return;
        }
        if (pedidoEmAndamento == null || pedidoEmAndamento.getPizzas().isEmpty()) {
            view.exibirAviso("Adicione pelo menos uma pizza ao pedido!");
            return;
        }

        try {
            if (pedidoEmEdicao != null) {
                pedidoEmEdicao.setPizzas(pedidoEmAndamento.getPizzas());
                pedidoDao.update(pedidoEmEdicao);
                view.exibirSucesso("Pedido #" + pedidoEmEdicao.getIdentificador() + " atualizado com sucesso!");
            } else {
                pedidoDao.add(clienteAtual.getId(), pedidoEmAndamento);
                view.exibirSucesso("Pedido #" + pedidoEmAndamento.getIdentificador() + " criado com sucesso!");
            }

            pedidoEmEdicao = null;
            pedidoEmAndamento = null;
            ocultarPainel();
            recarregarPedidosDoClienteAtual();
        } catch (PedidoNaoEditavelException e) {
            // O pedido mudou em outra tela (saiu para entrega, foi entregue ou excluído): nada foi gravado
            pedidoEmEdicao = null;
            pedidoEmAndamento = null;
            ocultarPainel();
            view.exibirAviso(e.getMessage() + "\nAs alterações feitas nesta tela não foram salvas.");
            atualizarHistoricoDoClienteAtual();
        } catch (Exception e) {
            view.exibirAviso("Erro ao salvar pedido no banco de dados: " + e.getMessage());
        }
    }

    public Cliente getClienteAtual() {
        return clienteAtual;
    }

    // Esconde o painel de montagem e o carrinho e encerra qualquer edição de pizza em andamento
    private void ocultarPainel() {
        indicePizzaEmEdicao = null;
        view.ocultarPainelMontagemECarrinho();
    }

    private void recarregarPedidosDoClienteAtual() throws Exception {
        if (clienteAtual != null) {
            clienteAtual = clienteDao.getById(clienteAtual.getId());
            if (clienteAtual != null) {
                modeloPedidos.setListaPedidos(clienteAtual.getPedidos());
            }
        }
    }

    private void atualizarHistoricoDoClienteAtual() {
        try {
            recarregarPedidosDoClienteAtual();
        } catch (Exception e) {
            view.exibirAviso("Erro ao recarregar os pedidos do cliente: " + e.getMessage());
        }
    }

    private void atualizarCarrinhoNaView() {
        if (pedidoEmAndamento == null) {
            modeloCarrinho.setListaPizzas(Collections.emptyList());
            view.atualizarLabelPrecoTotal(0.0);
        } else {
            modeloCarrinho.setListaPizzas(pedidoEmAndamento.getPizzas());
            view.atualizarLabelPrecoTotal(pedidoEmAndamento.calcularPrecoTotal());
        }
    }
}
