package sistemapizzaria.controller;

import java.util.ArrayList;
import java.util.List;

import sistemapizzaria.cliente.Cliente;
import sistemapizzaria.cliente.Pedido;
import sistemapizzaria.cliente.StatusPedido;
import sistemapizzaria.dao.ClienteDao;
import sistemapizzaria.dao.DaoFactory;
import sistemapizzaria.dao.PedidoDao;
import sistemapizzaria.excecoes.PedidoNaoEditavelException;
import sistemapizzaria.model.GerenciarPedidosTableModel;
import sistemapizzaria.model.GerenciarPedidosTableModel.LinhaPedidoCliente;
import sistemapizzaria.telas.TelaGerenciarPedidos;

/**
 * Controlador responsável pela visualização de todos os pedidos e alteração de seus estados (Padrão MVC - Aula 06).
 */
public class GerenciarPedidosController {

    private final TelaGerenciarPedidos view;
    private final GerenciarPedidosTableModel tableModel;
    private final ClienteDao clienteDao;
    private final PedidoDao pedidoDao;

    public GerenciarPedidosController(TelaGerenciarPedidos view, GerenciarPedidosTableModel tableModel) {
        this.view = view;
        this.tableModel = tableModel;
        this.clienteDao = DaoFactory.getClienteDao();
        this.pedidoDao = DaoFactory.getPedidoDao();
    }

    public void atualizarTabela(String telefoneFiltro, StatusPedido statusFiltro) {
        try {
            String tel = (telefoneFiltro != null) ? telefoneFiltro.trim() : "";
            List<Cliente> clientes = clienteDao.getAll();
            List<LinhaPedidoCliente> linhas = new ArrayList<>();

            for (Cliente c : clientes) {
                if (!tel.isEmpty() && (c.getTelefone() == null || !c.getTelefone().contains(tel))) {
                    continue;
                }
                for (Pedido p : c.getPedidos()) {
                    if (statusFiltro != null && p.getStatus() != statusFiltro) {
                        continue;
                    }
                    linhas.add(new LinhaPedidoCliente(c, p));
                }
            }
            tableModel.setLinhas(linhas);
        } catch (Exception e) {
            view.exibirAviso("Erro ao carregar pedidos do banco de dados: " + e.getMessage());
        }
    }

    public void alterarStatusPedido(int linhaSelecionada, StatusPedido novoStatus, String telefoneFiltro, StatusPedido statusFiltro) {
        if (linhaSelecionada == -1) {
            view.exibirAviso("Selecione um pedido na tabela.");
            return;
        }
        Pedido pedido = tableModel.getPedido(linhaSelecionada);
        if (pedido == null || novoStatus == null) {
            view.exibirAviso("Não foi possível atualizar o pedido.");
            return;
        }
        try {
            pedido.atualizarStatus(novoStatus);
            pedidoDao.updateStatus(pedido.getIdentificador(), novoStatus);
            atualizarTabela(telefoneFiltro, statusFiltro);
            view.exibirSucesso("Estado do pedido #" + pedido.getIdentificador() + " atualizado para " + novoStatus + "!");
        } catch (PedidoNaoEditavelException e) {
            // O pedido foi excluído em outra tela: a tabela é atualizada para refletir isso
            atualizarTabela(telefoneFiltro, statusFiltro);
            view.exibirAviso(e.getMessage());
        } catch (Exception e) {
            view.exibirAviso("Erro ao atualizar estado do pedido: " + e.getMessage());
        }
    }
}
