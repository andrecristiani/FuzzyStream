package FuzzyProject.FuzzyDT.Models;

import FuzzyProject.FuzzyDT.Utils.ConverteArquivos;
import FuzzyProject.FuzzyDT.Utils.ManipulaArquivos;
import FuzzyProject.FuzzyND.Models.Exemplo;
import FuzzyProject.FuzzyND.Models.SPFMiC;
import FuzzyProject.FuzzyND.Models.TipicidadeMaxima;
import FuzzyProject.FuzzyND.Utils.MedidasDeDistancia;

import java.util.*;

public class ComiteArvores {

    public int tamanhoMaximo;
    public int numAtributos;
    public String dataset;
    public String caminho;
    public String taxaPoda;
    public int numCjtos;
    public double m;
    public double n;
    public int K;
    public TipicidadeMaxima todasTipMax;
    public double adaptadorTheta = 0;
    public List<String> atributos = new ArrayList<>();
    public List<DecisionTree> modelos = new ArrayList<>();
    public List<String> rotulosConhecidos = new ArrayList<>();
    public List<String> rotulosSPFMiCs = new ArrayList<>();
    public List<Integer> numeroClassificadores = new ArrayList<>();
    public Map<String, Integer> hashmapRotulos = new HashMap<>(); //para converter String para Integer

    public FDT fdt = new FDT();
    public ConverteArquivos ca = new ConverteArquivos();
    public ManipulaArquivos ma = new ManipulaArquivos();

    public ComiteArvores(String dataset, String caminho, String taxaPoda, int numCjtos, int tamanhoMaximo) {
        this.dataset = dataset;
        this.caminho = caminho;
        this.taxaPoda = taxaPoda;
        this.numCjtos = numCjtos;
        this.tamanhoMaximo = tamanhoMaximo;
    }

    public void treinaComiteInicialKMeans(int tChunk, int K, int tempo) throws Exception {
        ResultadoMA rMA = ca.main(this.dataset, this, tChunk);
        for(int i=0; i< rMA.numClassificadores; i++) {
            DecisionTree dt = new DecisionTree(this.caminho, this.dataset, i, this.taxaPoda, tempo);
            dt.numObjetos = ma.getNumExemplos(this.caminho+this.dataset + i + ".txt");
            dt.numAtributos = this.numAtributos;
            dt.atributos = this.atributos;
            fdt.geraFuzzyDT(this.dataset + i, this.taxaPoda, this.numCjtos, this.caminho, dt);
            fdt.criaGruposEmNosFolhasKMeans(this.dataset+i, this.caminho, dt, tChunk, K);
            ma.apagaArqsTemporarios(dataset + i, caminho);
            this.modelos.add(dt);
        }
    }

    public void treinaComiteInicialFuzzyCMeans(int tChunk, int K, double fuzzificacao, double alpha, double theta, int tempo) throws Exception {
        ResultadoMA rMA = ca.main(this.dataset, this, tChunk);
        for(int i=0; i< rMA.numClassificadores; i++) {
            try {
                DecisionTree dt = new DecisionTree(this.caminho, this.dataset, i, this.taxaPoda, i);
                dt.numObjetos = ma.getNumExemplos(this.caminho + this.dataset + i + ".txt");
                dt.numAtributos = this.numAtributos;
                dt.atributos = this.atributos;
                dt.rotulos = rMA.rotulos.get(i);
                fdt.geraFuzzyDT(this.dataset + i, this.taxaPoda, this.numCjtos, this.caminho, dt);
                fdt.criaGruposEmNosFolhasFuzzyCMeans(this.dataset + i, this.caminho, dt, tChunk, K, fuzzificacao, alpha, theta, this);
                ma.apagaArqsTemporarios(dataset + i, caminho);
                this.modelos.add(dt);
            } catch (Exception e) {
                System.err.println("Impossível treinar árvore com apenas uma classe");
            }
        }
    }

    public String classificaExemploAgrupamentoExternoKMeans(double[] exemplo) {
        if(this.calculaFoutlierKmeans(exemplo)) {
            return "desconhecido";
        } else {
            Map<String, Integer> numeroVotos = new HashMap<>();
            for (int i = 0; i < rotulosConhecidos.size(); i++) {
                numeroVotos.put(rotulosConhecidos.get(i), 0);
            }

            Vector v = new Vector<>();
            for (int i = 0; i < exemplo.length; i++) {
                v.add(exemplo[i]);
            }

            for (int i = 0; i < modelos.size(); i++) {
                String rotuloVotado = fdt.classificaExemploSemGruposNosFolhas(modelos.get(0), v);
                numeroVotos.replace(rotuloVotado, numeroVotos.get(rotuloVotado) + 1);
            }

            int valorMaior = -1;
            String indiceMaior = null;

            for(int i=0; i<numeroVotos.size(); i++) {
                String rotulo = rotulosConhecidos.get(i);
                if(valorMaior < numeroVotos.get(rotulo)) {
                    valorMaior = numeroVotos.get(rotulo);
                    indiceMaior = rotulo;
                }
            }
            return indiceMaior;
        }
    }

    public String classificaExemploAgrupamentoExternoFuzzyCMeans(double[] exemplo) {
        if(this.calculaFoutlierPelaTipicidade(exemplo)) {
            return "desconhecido";
        } else {
            Map<String, Integer> numeroVotos = new HashMap<>();
            List<String> rotulos = new ArrayList<String>();
//            for (int i = 0; i < hashmapRotulos.size(); i++) {
//                numeroVotos.put(String.valueOf(i), 0);
//                rotulos.add(String.valueOf(i));
//            }

            Vector v = new Vector<>();
            for (int i = 0; i < exemplo.length; i++) {
                v.add(exemplo[i]);
            }

            if(this.modelos.size() == 1) {
                return fdt.classificaExemploSemGruposNosFolhas(modelos.get(0), v);
            }

            String votoMaisNovo = "";
            int tempoMaisNovo = 0;
            List<String> votos = new ArrayList<>();
            for (int i = 0; i < modelos.size(); i++) {
                String rotuloVotado = fdt.classificaExemploSemGruposNosFolhas(modelos.get(i), v);
//                int t = this.hashmapRotulos.get(rotuloVotado);
//                int novoValor = numeroVotos.get(rotuloVotado) + 1;
                if(numeroVotos.containsKey(rotuloVotado)) {
                    numeroVotos.replace(rotuloVotado, numeroVotos.get(rotuloVotado) + 1);
                } else {
                    numeroVotos.put(rotuloVotado, 1);
                }
                votos.add(rotuloVotado);
                if(modelos.get(i).t > tempoMaisNovo) {
                    tempoMaisNovo = modelos.get(i).t;
                    votoMaisNovo = rotuloVotado;
                }
            }

            int valorMaior = -1;
            String indiceMaior = null;

            rotulos.addAll(numeroVotos.keySet());
            for(int i=0; i<numeroVotos.size(); i++) {
                String rotulo = rotulos.get(i);
                if(valorMaior < numeroVotos.get(rotulo)) {
                    valorMaior = numeroVotos.get(rotulo);
                    indiceMaior = rotulo;
                }
            }

            if(valorMaior == this.modelos.size()) {
                return indiceMaior;
            } else {
                int contadorClasseDesconhecida = 0;
                int contadorMesmoVotoQueMaisNovo = 0;
                for(int i=0; i<modelos.size(); i++) {
                    if(votos.get(i) != votoMaisNovo) {
                        contadorClasseDesconhecida++;
                    } else {
                        contadorMesmoVotoQueMaisNovo++;
                    }

                    if(contadorClasseDesconhecida == (this.modelos.size() - contadorMesmoVotoQueMaisNovo)) {
                        return votoMaisNovo;
                    }
                }
            }
            return indiceMaior;
        }
    }

    public String classificaExemploAgrupamentoNoFolha(double[] exemplo) {
        Map<String, Integer> numeroVotos = new HashMap<>();
        for(int i=0; i<rotulosConhecidos.size(); i++) {
            numeroVotos.put(rotulosConhecidos.get(i), 0);
        }
        numeroVotos.put("desconhecido", 0);

        Vector v = new Vector<>();
        for(int i=0; i<exemplo.length; i++) {
            v.add(exemplo[i]);
        }

        for(int i=0; i<modelos.size(); i++) {
            String rotuloVotado = fdt.classificaExemploFuzzyCMeans(modelos.get(0), v);
            numeroVotos.replace(rotuloVotado, numeroVotos.get(rotuloVotado) + 1);
        }

        if(numeroVotos.get("desconhecido") == this.modelos.size()) {
            return "desconhecido";
        } else {
            numeroVotos.remove("desconhecido");
            int valorMaior = -1;
            String indiceMaior = null;

            for(int i=0; i<numeroVotos.size(); i++) {
                String rotulo = rotulosConhecidos.get(i);
                if(valorMaior < numeroVotos.get(rotulo)) {
                    valorMaior = numeroVotos.get(rotulo);
                    indiceMaior = rotulo;
                }
            }
            return indiceMaior;
        }
    }

    private void removeClassificadorComMenorDesempenho(List<Exemplo> exemplosRotulados) {
        double[] pontuacaoArvores = new double[this.modelos.size()];
        for(int i=0; i<this.modelos.size(); i++) {
            pontuacaoArvores[i] = 0;
        }

        for(int i=0; i<exemplosRotulados.size(); i++) {
            Vector v = new Vector<>();
            double[] exemplo = exemplosRotulados.get(i).getPoint();
            for(int j=0; j<exemplo.length; j++) {
                v.add(exemplo[j]);
            }
            for(int k=0; k<this.modelos.size(); k++) {
                String rotuloClassificado = this.fdt.classificaExemploSemGruposNosFolhas(this.modelos.get(k), v);
                String rotuloVerdadeiro = exemplosRotulados.get(i).getRotuloVerdadeiro();

                if(rotuloClassificado.equals(rotuloVerdadeiro)) {
                    pontuacaoArvores[k]++;
                }
            }
        }

        List<Double> acuraciaArvores = new ArrayList<>();
        double acuraciaMinima = 100;
        int index = 0;
        for(int i=0; i<this.modelos.size(); i++) {
            double acuracia = ((pontuacaoArvores[i]/exemplosRotulados.size())*100);
            acuraciaArvores.add(acuracia);
            if(acuracia < acuraciaMinima) {
                acuraciaMinima = acuracia;
                index = i;
            }
        }
        this.modelos.remove(index);
    }

    private void atualizaRotulosConhecidos() {
        this.rotulosConhecidos.clear();
        for(int i=0; i<this.modelos.size(); i++) {
            for(int j=0; j<this.modelos.get(i).rotulos.size(); j++) {
                if(!this.rotulosConhecidos.contains(this.modelos.get(i).rotulos.get(j))) {
                    this.rotulosConhecidos.add(this.modelos.get(i).rotulos.get(j));
                }
            }
        }
    }

    public void treinaNovaArvore(List<Exemplo> exemplosRotulados, int tChunk, int K, int tempo) throws Exception {
        if(this.modelos.size() >= tamanhoMaximo) {
            this.removeClassificadorComMenorDesempenho(exemplosRotulados);
        }
        ResultadoMA resultadoMA = ca.mainParaExemplosRotulados(this.dataset, exemplosRotulados, this, tChunk);
        DecisionTree dt = new DecisionTree(this.caminho, this.dataset, resultadoMA.numClassificadores, this.taxaPoda, tempo);
        dt.numObjetos = ma.getNumExemplos(this.caminho+this.dataset + resultadoMA.numClassificadores + ".txt");
        dt.numAtributos = this.numAtributos;
        dt.atributos = this.atributos;
        fdt.geraFuzzyDT(this.dataset + resultadoMA.numClassificadores, this.taxaPoda, this.numCjtos, this.caminho, dt);
        fdt.criaGruposEmNosFolhasKMeans(this.dataset+resultadoMA.numClassificadores, this.caminho, dt, tChunk, K);
        ma.apagaArqsTemporarios(dataset + resultadoMA.numClassificadores, caminho);
        this.modelos.add(dt);
        dt.elementosPorRegraKMeans.clear();
    }

    public void treinaNovaArvoreFuzzyCMeans(List<Exemplo> exemplosRotulados, int tChunk, int K, double fuzzificacao, double alpha, double theta, int tempo) throws Exception {

        try {
            ResultadoMA resultadoMA = ca.mainParaExemplosRotulados(this.dataset, exemplosRotulados, this, tChunk);
            DecisionTree dt = new DecisionTree(this.caminho, this.dataset, resultadoMA.numClassificadores, this.taxaPoda, tempo);
            dt.numObjetos = ma.getNumExemplos(this.caminho + this.dataset + resultadoMA.numClassificadores + ".txt");
            dt.numAtributos = this.numAtributos;
            dt.atributos = this.atributos;
            dt.rotulos = resultadoMA.rotulos.get(0);
            fdt.geraFuzzyDT(this.dataset + resultadoMA.numClassificadores, this.taxaPoda, this.numCjtos, this.caminho, dt);
            fdt.criaGruposEmNosFolhasFuzzyCMeans(this.dataset + resultadoMA.numClassificadores, this.caminho, dt, tChunk, K, fuzzificacao, alpha, theta, this);
            ma.apagaArqsTemporarios(dataset + resultadoMA.numClassificadores, caminho);
            if(this.modelos.size() >= tamanhoMaximo) {
                this.removeClassificadorComMenorDesempenho(exemplosRotulados);
            }
            this.modelos.add(dt);
            dt.elementosPorRegraKMeans.clear();
        } catch (Exception e) {
            System.err.println("Impossível treinar árvore com apenas uma classe");
        }
    }

    public boolean calculaFoutlierKmeans(double[] exemplo) {
        double distExemploCentroide = 0;
        for(int i=0; i<this.modelos.size(); i++) {
            List<List<MicroGrupo>> microGruposPorRegra = this.modelos.get(i).microGruposPorRegra;
            for(int j=0; j<microGruposPorRegra.size(); j++) {
                List<MicroGrupo> regras = microGruposPorRegra.get(j);
                for(int k=0; k<regras.size(); k++) {
                    distExemploCentroide = MedidasDeDistancia.calculaDistanciaEuclidiana(exemplo, regras.get(k).getCentroide());
                    if (distExemploCentroide <= regras.get(k).raio) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean calculaFoutlierFuzzyCMeans(Vector exemplo) {
        for(int i=0; i<this.modelos.size(); i++) {
            List<List<SPFMiC>> sfmic = this.modelos.get(i).sfMicPorRegra;
            for(int j=0; j<sfmic.size(); j++) {
                List<SPFMiC> regras = sfmic.get(j);
                for(int k=0; k<regras.size(); k++) {
                    //if (regras.get(i).verificaSeExemploPertenceAoGrupo(exemplo)) {
                        return false;
                   // }
                }
            }
        }

        return true;
    }

    public boolean calculaFoutlierPelaTipicidade(double[] exemplo) {
        List<SPFMiC> todosSFMiCs = this.getTodosSFMiCs();
        List<Double> tipicidades = new ArrayList<>();
        for(int i=0; i<todosSFMiCs.size(); i++) {
            tipicidades.add(todosSFMiCs.get(i).calculaTipicidade(exemplo, this.n, this.K));
        }

        Double maxVal = Collections.max(tipicidades);
        double limiar = (this.todasTipMax.getValorMedio() - this.adaptadorTheta);
//        System.err.println("Limiar: " + limiar + "      MaxVal: " + maxVal);
        if(maxVal >= limiar) {
            this.todasTipMax.addValorTipicidade(maxVal);
            return false;
        }
        return true;
    }

    public boolean calculaFoutlierPelaDispersao(double[] exemplo) {
        List<SPFMiC> todosSFMiCs = this.getTodosSFMiCs();
        for(int i=0; i<todosSFMiCs.size(); i++) {
            double distancia = MedidasDeDistancia.calculaDistanciaEuclidiana(exemplo, todosSFMiCs.get(i).getCentroide());
            if(distancia <= todosSFMiCs.get(i).getDispersao()) {
                return false;
            }
        }
        return true;
    }

    public List<SPFMiC> getTodosSFMiCs() {
        List<SPFMiC> todosSFMiCs = new ArrayList<>();
        for(int i=0; i<this.modelos.size(); i++) {
            List<List<SPFMiC>> sfmicsPorRegras = this.modelos.get(i).sfMicPorRegra;
            for(int j=0; j<sfmicsPorRegras.size(); j++) {
                todosSFMiCs.addAll(sfmicsPorRegras.get(j));
            }
        }

        return todosSFMiCs;
    }
}
