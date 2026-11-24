package sistemapizzaria.formas;

import sistemapizzaria.excecoes.*;

public class TrianguloEquilatero extends Forma {

    private double lado;
    // Estabelece a restrição mínima para a dimensão do lado do triângulo.
    public static final double MIN_LADO = 20.0;
    // Estabelece a restrição máxima para a dimensão do lado do triângulo.
    public static final double MAX_LADO = 60.0;

    // Define o comprimento do lado mediante checagem de conformidade com os limites exigidos.
    @Override
    public void definirPorDimensao(double dimensao) {
        // Avalia se o parâmetro (inclusive valores não numéricos) encontra-se fora do intervalo permitido.
        if (Double.isNaN(dimensao) || dimensao < MIN_LADO || dimensao > MAX_LADO) {
            // Aborta a execução e notifica a violação da restrição.
            throw new DimensaoInvalidaException("O lado do triângulo deve ter entre 20 e 60 cm.");
        }
        // Atribui o dado consistente à variável de escopo global da classe.
        this.lado = dimensao;
    }

    // Deriva a dimensão do lado utilizando a área requerida
    @Override
    public void definirPorArea(double area) {
        // Valida a área contra a faixa que o triângulo aceita (limites globais de área e limites do lado)
        if (Double.isNaN(area) || area < getAreaMinima() || area > getAreaMaxima()) {
            throw new AreaInvalidaException("A área de uma pizza triangular deve ter entre " + descreverFaixaDeArea() + ".");
        }

        // Isola a variável lado na fórmula da área do triângulo equilátero para realizar a obtenção reversa da dimensão.
        // Dentro da faixa de área aceita o lado sempre respeita os limites; o ajuste abaixo apenas protege contra
        // imprecisão de ponto flutuante nas bordas.
        double ladoCalculado = Math.sqrt((4 * area) / Math.sqrt(3));
        this.lado = Math.min(MAX_LADO, Math.max(MIN_LADO, ladoCalculado));
    }

    // Calcula área do triângulo equilátero utilizando o valor armazenado no atributo de lado.
    @Override
    public double calcularArea() {
        // Fórmula da geometria para triângulos equiláteros
        return areaDoLado(this.lado);
    }

    @Override
    public double getAreaMinima() {
        return Math.max(MIN_AREA, areaDoLado(MIN_LADO));
    }

    @Override
    public double getAreaMaxima() {
        return Math.min(MAX_AREA, areaDoLado(MAX_LADO));
    }

    // Área de um triângulo equilátero com o lado informado.
    private static double areaDoLado(double lado) {
        return (Math.pow(lado, 2) * Math.sqrt(3)) / 4.0;
    }

    // Retorna a medida registrada para o lado do triângulo.
    public double getLado() {
        return this.lado;
    }
}
