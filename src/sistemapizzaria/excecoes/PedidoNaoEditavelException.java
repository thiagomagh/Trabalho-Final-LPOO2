package sistemapizzaria.excecoes;

// Exceção lançada quando se tenta alterar um pedido que não está mais aberto (já saiu para entrega, foi entregue ou foi excluído).
public class PedidoNaoEditavelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PedidoNaoEditavelException(String mensagem) {
        super(mensagem);
    }
}
