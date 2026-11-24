package sistemapizzaria.formas;

import sistemapizzaria.excecoes.*;

public class Quadrado extends Forma {

    private double lado;
    // Estabelece o limite mínimo aceitável para o lado do quadrado.
    public static final double MIN_LADO = 10.0;
    // Estabelece o limite máximo aceitável para o lado do quadrado.
    public static final double MAX_LADO = 40.0;

    // Ajusta a medida do lado do quadrado.
    @Override
    public void definirPorDimensao(double dimensao) {
        // Verifica a conformidade da dimensão recebida (inclusive valores não numéricos) em relação aos limites estipulados para a forma.
        if (Double.isNaN(dimensao) || dimensao < MIN_LADO || dimensao > MAX_LADO) {
            // Interrompe o fluxo de execução lançando um erro de dimensão inválida.
            throw new DimensaoInvalidaException("O lado do quadrado deve ter entre 10 e 40 cm.");
        }
        // Efetiva a gravação do dado validado no atributo da instância.
        this.lado = dimensao;
    }

    @Override
    public void definirPorArea(double area) {
        // Confirma se a área submetida está dentro da faixa que o quadrado aceita (limites globais de área e limites do lado).
        if (Double.isNaN(area) || area < getAreaMinima() || area > getAreaMaxima()) {
            // Lança um erro de área inválida para evitar cálculos desnecessários e estados inconsistentes.
            throw new AreaInvalidaException("A área de uma pizza quadrada deve ter entre " + descreverFaixaDeArea() + ".");
        }

        // Extrai o valor do lado. Dentro da faixa de área aceita o lado sempre respeita os limites;
        // o ajuste abaixo apenas protege contra imprecisão de ponto flutuante nas bordas.
        this.lado = Math.min(MAX_LADO, Math.max(MIN_LADO, Math.sqrt(area)));
    }

    // Calcula a forma geométrica do quadrado
    @Override
    public double calcularArea() {
        return Math.pow(this.lado, 2);
    }

    @Override
    public double getAreaMinima() {
        return Math.max(MIN_AREA, MIN_LADO * MIN_LADO);
    }

    @Override
    public double getAreaMaxima() {
        return Math.min(MAX_AREA, MAX_LADO * MAX_LADO);
    }

    // Retorna a medida atual configurada para o lado do quadrado.
    public double getLado() {
        return this.lado;
    }
}
