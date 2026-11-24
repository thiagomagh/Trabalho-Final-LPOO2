package sistemapizzaria.controller;

import java.util.List;

import sistemapizzaria.cliente.Cliente;
import sistemapizzaria.dao.ClienteDao;
import sistemapizzaria.dao.DaoFactory;
import sistemapizzaria.model.ClienteTableModel;
import sistemapizzaria.telas.TelaClientes;

/**
 * Controlador responsável pelo gerenciamento de Clientes (Padrão MVC - Aula 06).
 */
public class ClienteController {

    // Nome e sobrenome começam com uma letra e aceitam letras (com acento), espaço, ponto, hífen e apóstrofo (' ou ’)
    private static final String PADRAO_NOME = "\\p{L}[\\p{L} .'\\u2019\\-]*";
    // Mesmo tamanho declarado nas colunas nome/sobrenome da tabela clientes
    private static final int TAMANHO_MAXIMO_NOME = 100;

    private final TelaClientes view;
    private final ClienteTableModel tableModel;
    private final ClienteDao clienteDao;
    private Cliente clienteEmEdicao;

    public ClienteController(TelaClientes view, ClienteTableModel tableModel) {
        this.view = view;
        this.tableModel = tableModel;
        this.clienteDao = DaoFactory.getClienteDao();
    }

    public void atualizarTabela() {
        try {
            List<Cliente> clientes = clienteDao.getAll();
            tableModel.setListaClientes(clientes);
        } catch (Exception e) {
            view.exibirAviso("Erro ao carregar clientes do banco de dados: " + e.getMessage());
        }
    }

    public void filtrarClientes(String filtroSobrenome, String filtroTelefone) {
        try {
            List<Cliente> filtrados = clienteDao.buscarPorSobrenomeETelefone(filtroSobrenome, filtroTelefone);
            tableModel.setListaClientes(filtrados);
        } catch (Exception e) {
            view.exibirAviso("Erro ao filtrar clientes: " + e.getMessage());
        }
    }

    public void filtrarClientesTextoUnico(String termo) {
        try {
            List<Cliente> filtrados = clienteDao.buscarPorFiltro(termo);
            tableModel.setListaClientes(filtrados);
        } catch (Exception e) {
            view.exibirAviso("Erro ao pesquisar clientes: " + e.getMessage());
        }
    }

    public void salvarCliente(String nome, String sobrenome, String telefone, boolean modoEdicao) {
        try {
            if (nome == null || nome.trim().isEmpty()) {
                throw new IllegalArgumentException("O nome do cliente não pode estar vazio!");
            }
            validarNomeDePessoa(nome.trim(), "nome");

            if (sobrenome == null || sobrenome.trim().isEmpty()) {
                throw new IllegalArgumentException("O sobrenome do cliente não pode estar vazio!");
            }
            validarNomeDePessoa(sobrenome.trim(), "sobrenome");

            if (telefone == null || telefone.trim().isEmpty()) {
                throw new IllegalArgumentException("O telefone do cliente não pode estar vazio!");
            }

            String telefoneLimpo = telefone.replaceAll("[^0-9]", "");
            if (telefoneLimpo.length() < 10 || telefoneLimpo.length() > 11) {
                throw new IllegalArgumentException(
                    "Telefone inválido! Use o formato: DDD + Número (ex: 11987654321).\n" +
                    "Deve conter 10 ou 11 dígitos numéricos."
                );
            }

            String nomeLimpo = nome.trim();
            String sobrenomeLimpo = sobrenome.trim();

            if (modoEdicao && clienteEmEdicao != null) {
                Cliente existenteComMesmoTelefone = clienteDao.getByTelefone(telefoneLimpo);
                if (existenteComMesmoTelefone != null && existenteComMesmoTelefone.getId() != clienteEmEdicao.getId()) {
                    throw new IllegalArgumentException("Já existe outro cliente cadastrado com o telefone " + telefoneLimpo + "!");
                }
                clienteEmEdicao.setNome(nomeLimpo);
                clienteEmEdicao.setSobrenome(sobrenomeLimpo);
                clienteEmEdicao.setTelefone(telefoneLimpo);
                clienteDao.update(clienteEmEdicao);
                clienteEmEdicao = null;
                view.exibirSucesso("Informações do cliente '" + nomeLimpo + "' atualizadas com sucesso!");
            } else {
                Cliente existente = clienteDao.getByTelefone(telefoneLimpo);
                if (existente != null) {
                    throw new IllegalArgumentException("Erro: Já existe um cliente cadastrado com o telefone " + telefoneLimpo + "!");
                }
                Cliente novoCliente = new Cliente(nomeLimpo, sobrenomeLimpo, telefoneLimpo);
                clienteDao.add(novoCliente);
                view.exibirSucesso("Cliente '" + nomeLimpo + "' cadastrado com sucesso!");
            }

            view.sairModoEdicaoELimpar();
            atualizarTabela();
        } catch (IllegalArgumentException ex) {
            view.exibirAviso(ex.getMessage());
        } catch (Exception ex) {
            view.exibirAviso("Erro no banco de dados: " + ex.getMessage());
        }
    }

    // Nome/sobrenome: só letras (com acento), espaço, ponto, hífen e apóstrofo, com no máximo 100 caracteres
    private void validarNomeDePessoa(String valor, String campo) {
        if (valor.length() > TAMANHO_MAXIMO_NOME) {
            throw new IllegalArgumentException("O " + campo + " do cliente deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres!");
        }
        if (!valor.matches(PADRAO_NOME)) {
            throw new IllegalArgumentException(
                "O " + campo + " do cliente deve conter apenas letras (também são aceitos espaços, ponto, hífen e apóstrofo)!"
            );
        }
    }

    public void iniciarEdicao(int linhaSelecionada) {
        if (linhaSelecionada == -1) {
            view.exibirAviso("Por favor, selecione um cliente na tabela para editar.");
            return;
        }
        Cliente selecionado = tableModel.getCliente(linhaSelecionada);
        if (selecionado != null) {
            this.clienteEmEdicao = selecionado;
            view.preencherFormularioParaEdicao(selecionado);
        }
    }

    public void cancelarEdicao() {
        this.clienteEmEdicao = null;
        view.sairModoEdicaoELimpar();
    }

    public void excluirCliente(int linhaSelecionada, boolean modoEdicao) {
        if (modoEdicao) {
            view.exibirAviso("Você não pode excluir clientes enquanto o modo de edição estiver ativo. Termine ou cancele a edição.");
            return;
        }
        if (linhaSelecionada == -1) {
            view.exibirAviso("Por favor, selecione um cliente na tabela para excluir.");
            return;
        }
        Cliente selecionado = tableModel.getCliente(linhaSelecionada);
        if (selecionado == null) {
            return;
        }

        // Excluir o cliente apaga também todos os pedidos dele. Pede confirmação antes
        String pergunta = "Deseja realmente excluir o cliente '" + selecionado.getNome() + " " + selecionado.getSobrenome() + "'?";
        int qtdPedidos = selecionado.getPedidos().size();
        if (qtdPedidos > 0) {
            pergunta += "\nEle possui " + qtdPedidos + " pedido(s), que também serão excluídos.";
        }
        if (!view.confirmarAcao(pergunta)) {
            return;
        }

        try {
            clienteDao.delete(selecionado);
            view.exibirSucesso("Cliente '" + selecionado.getNome() + "' e seus pedidos foram excluídos com sucesso!");
            atualizarTabela();
        } catch (Exception e) {
            view.exibirAviso("Erro ao excluir cliente no banco de dados: " + e.getMessage());
        }
    }

    public Cliente obterClienteNaLinha(int linhaSelecionada) {
        return tableModel.getCliente(linhaSelecionada);
    }
}
