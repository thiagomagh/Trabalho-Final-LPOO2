package sistemapizzaria.cliente;

import java.util.ArrayList;
import java.util.List;

import sistemapizzaria.pizza.*;

public class Pedido {

    private int identificador;
    private double precoTotal;
    private StatusPedido status;

    // Total que está gravado no banco para este pedido (null quando o pedido ainda não veio do banco).
    // Enquanto o pedido está ABERTO o total acompanha a tabela de preços atual; depois que sai de ABERTO
    // (a caminho / entregue) vale este valor gravado, para que um reajuste de preços não altere vendas já fechadas.
    private final Double precoRegistrado;

    // Mantém a coleção de pizzas vinculadas a este respectivo pedido.
    private List<Pizza> pizzas;

    // Controla a geração sequencial e global de chaves de identificação
    private static int contadorId = 1;

    public Pedido() {
        // Atribui o valor atual do contador ao identificador da instância e incrementa a variável estática em seguida
        this.identificador = contadorId++;
        this.precoTotal = 0.0;
        this.precoRegistrado = null;
        this.status = StatusPedido.ABERTO;
        this.pizzas = new ArrayList<>();
    }

    public Pedido(int identificador, double precoTotal, StatusPedido status) {
        this.identificador = identificador;
        this.precoTotal = precoTotal;
        this.precoRegistrado = precoTotal;
        this.status = status != null ? status : StatusPedido.ABERTO;
        this.pizzas = new ArrayList<>();
        if (identificador >= contadorId) {
            contadorId = identificador + 1;
        }
    }

    public void setIdentificador(int identificador) {
        this.identificador = identificador;
        if (identificador >= contadorId) {
            contadorId = identificador + 1;
        }
    }

    public void setPizzas(List<Pizza> novasPizzas) {
        this.pizzas.clear();
        if (novasPizzas != null) {
            this.pizzas.addAll(novasPizzas);
        }
        this.calcularPrecoTotal();
    }

    public void adicionarPizza(Pizza pizza) {
        // Valida a consistência da referência recebida antes de processar a modificação da estrutura.
        if (pizza != null) {
            this.pizzas.add(pizza);
            // Atualiza preço total
            this.calcularPrecoTotal();
        }
    }

    public void removerPizza(Pizza pizza) {
        // Verifica se a referência apontada é autêntica antes de solicitar a remoção.
        if (pizza != null) {
            this.pizzas.remove(pizza);
            this.calcularPrecoTotal();
        }
    }

    // Calcula o total do pedido com os preços por cm² vigentes agora (é o valor gravado enquanto o pedido está aberto
    // e o valor congelado no momento em que ele é fechado).
    public double calcularPrecoTotal() {
        // Necessário reinicializar a variável para cada novo cálculo
        double soma = 0.0;

        for (Pizza pizza : this.pizzas) {
            // Invoca o método polimórfico de cálculo e acumula o resultado.
            soma += pizza.calcularPreco();
        }

        this.precoTotal = soma;

        return this.precoTotal;
    }

    // Altera o estágio de acompanhamento do pedido no sistema.
    public void atualizarStatus(StatusPedido novoStatus) {
        // Previne a atribuição de estados inconsistentes
        if (novoStatus != null) {
            this.status = novoStatus;
        }
    }

    public int getIdentificador() {
        return identificador;
    }

    // Total do pedido: pedido aberto é recalculado com a tabela de preços atual; pedido a caminho ou entregue
    // mantém o total registrado na época da venda (não muda quando o preço do cm² é atualizado).
    public double getPrecoTotal() {
        if (this.status != StatusPedido.ABERTO && this.precoRegistrado != null) {
            return this.precoRegistrado;
        }
        return calcularPrecoTotal();
    }

    public StatusPedido getStatus() {
        return status;
    }

    public List<Pizza> getPizzas() {
        return new ArrayList<>(this.pizzas);
    }
}
