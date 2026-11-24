package sistemapizzaria.pizza;

public class TipoPizza {

    private CategoriaTipo categoria;
    private double precoPorCm2;

    public TipoPizza(CategoriaTipo categoria, double precoPorCm2) {
        this.categoria = categoria;
        this.precoPorCm2 = precoPorCm2;
    }

    public void atualizarPrecoPorCm2(double preco) {
        // Rejeita valores que não são números finitos (NaN e infinito), que corromperiam todos os preços calculados
        if (Double.isNaN(preco) || Double.isInfinite(preco)) {
            throw new IllegalArgumentException("O preço por centímetro quadrado deve ser um número válido.");
        }
        // Verifica se o valor financeiro fornecido é maior ou igual a zero
        if (preco >= 0.0) {
            this.precoPorCm2 = preco;
        } else {
            throw new IllegalArgumentException("O preço por centímetro quadrado não pode ser negativo.");
        }
    }

    // Retorna a enumeração constante que identifica a categoria vinculada
    public CategoriaTipo getCategoria() {
        return categoria;
    }

    public double getPrecoPorCm2() {
        return precoPorCm2;
    }

    // Formata a cadeia de caracteres para exibição otimizada em componentes visuais
    @Override
    public String toString() {
        return categoria.name() + " - R$ " + String.format("%.2f", precoPorCm2) + " / cm²";
    }
}
