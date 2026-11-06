package caixa

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

private class LinhaMes(val mes: YearMonth, val entradas: Long, val saidas: Long, val saldoFim: Long?) {
    val resultado get() = entradas - saidas
}

@Composable
fun TelaResumo(dados: Dados) {
    var ano by remember { mutableStateOf(LocalDate.now().year) }
    val calc = remember(dados) { Calculo(dados) }
    val hoje = YearMonth.now()
    val inicio = YearMonth.from(LocalDate.parse(dados.dataSaldoInicial))

    val linhas = remember(dados, ano) {
        (1..12).map { m ->
            val ym = YearMonth.of(ano, m)
            val doMes = calc.doMes(ym)
            LinhaMes(
                mes = ym,
                entradas = doMes.filter { it.entrada }.sumOf { it.centavos },
                saidas = doMes.filter { !it.entrada }.sumOf { it.centavos },
                saldoFim = if ((ym.isAfter(hoje) || ym.isBefore(inicio)) && doMes.isEmpty()) null else calc.saldoAte(ym.atEndOfMonth()),
            )
        }
    }
    val doAno = remember(dados, ano) { dados.lancamentos.filter { it.dataLocal.year == ano } }
    val totalEntradas = linhas.sumOf { it.entradas }
    val totalSaidas = linhas.sumOf { it.saidas }

    val compacto = LocalCompacto.current
    val cabecalho: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Titulo(if (compacto) "Resumo" else "Resumo Mensal")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { ano-- }) { Icon(Icons.Filled.ChevronLeft, "Ano anterior", tint = Azul) }
            Text("$ano", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Azul)
            IconButton(onClick = { ano++ }) { Icon(Icons.Filled.ChevronRight, "Próximo ano", tint = Azul) }
        }
    }
    val corResultado = if (totalEntradas - totalSaidas < 0) Vermelho else Azul

    if (compacto) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cabecalho()
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CartaoValor("Entradas em $ano", moeda(totalEntradas), Verde, Modifier.weight(1f).fillMaxHeight())
                CartaoValor("Saídas em $ano", moeda(totalSaidas), Vermelho, Modifier.weight(1f).fillMaxHeight())
            }
            CartaoValor("Resultado do ano", moeda(totalEntradas - totalSaidas), corResultado, Modifier.fillMaxWidth())
            CartaoBranco(Modifier.fillMaxWidth()) {
                linhas.forEachIndexed { i, l ->
                    if (i > 0) HorizontalDivider(color = Color(0xFFEEEEEE))
                    MesCompacto(l, destaque = l.mes == hoje)
                }
            }
            CartaoBranco(Modifier.fillMaxWidth()) {
                TotaisPorCategoria("Entradas por categoria", dados.categoriasEntrada, doAno.filter { it.entrada }, Verde)
            }
            CartaoBranco(Modifier.fillMaxWidth()) {
                TotaisPorCategoria("Saídas por categoria", dados.categoriasSaida, doAno.filter { !it.entrada }, Vermelho)
            }
            Spacer(Modifier.height(8.dp))
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        cabecalho()
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CartaoValor("Entradas em $ano", moeda(totalEntradas), Verde, Modifier.weight(1f))
            CartaoValor("Saídas em $ano", moeda(totalSaidas), Vermelho, Modifier.weight(1f))
            CartaoValor("Resultado do ano", moeda(totalEntradas - totalSaidas), corResultado, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CartaoBranco(Modifier.weight(1.25f).fillMaxHeight()) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LinhaTabela(listOf("Mês", "Entradas", "Saídas", "Resultado", "Saldo no fim do mês"), cabecalho = true)
                    linhas.forEach { l ->
                        LinhaTabela(
                            listOf(
                                NOMES_MESES[l.mes.monthValue - 1],
                                if (l.entradas != 0L) moeda(l.entradas) else "–",
                                if (l.saidas != 0L) moeda(l.saidas) else "–",
                                if (l.entradas != 0L || l.saidas != 0L) moeda(l.resultado) else "–",
                                l.saldoFim?.let { moeda(it) } ?: "",
                            ),
                            cores = listOf(
                                Color.Black, Verde, Vermelho,
                                if (l.resultado < 0) Vermelho else Color.Black,
                                if ((l.saldoFim ?: 0L) < 0L) Vermelho else Color.Black,
                            ),
                            destaque = l.mes == hoje,
                        )
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                    LinhaTabela(
                        listOf("Total do ano", moeda(totalEntradas), moeda(totalSaidas), moeda(totalEntradas - totalSaidas), ""),
                        cores = listOf(Azul, Verde, Vermelho, Azul, Azul),
                        negrito = true,
                    )
                }
            }
            CartaoBranco(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                    TotaisPorCategoria("Entradas por categoria", dados.categoriasEntrada, doAno.filter { it.entrada }, Verde)
                    Spacer(Modifier.height(12.dp))
                    TotaisPorCategoria("Saídas por categoria", dados.categoriasSaida, doAno.filter { !it.entrada }, Vermelho)
                }
            }
        }
    }
}

@Composable
private fun MesCompacto(l: LinhaMes, destaque: Boolean) {
    val temMovimento = l.entradas != 0L || l.saidas != 0L
    Column(
        Modifier.fillMaxWidth().background(if (destaque) Color(0xFFFFF8E1) else Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(NOMES_MESES[l.mes.monthValue - 1], fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            l.saldoFim?.let {
                Text(
                    "Saldo ${moeda(it)}", style = MaterialTheme.typography.bodyMedium,
                    color = if (it < 0) Vermelho else Color.Black, maxLines = 1, softWrap = false,
                )
            }
        }
        if (temMovimento) {
            Row {
                Text("+ ${moeda(l.entradas)}", color = Verde, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(10.dp))
                Text("− ${moeda(l.saidas)}", color = Vermelho, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(
                    "= ${moeda(l.resultado)}", style = MaterialTheme.typography.bodySmall,
                    color = if (l.resultado < 0) Vermelho else Cinza,
                )
            }
        } else {
            Text("sem movimento", style = MaterialTheme.typography.bodySmall, color = Cinza)
        }
    }
}

@Composable
private fun TotaisPorCategoria(titulo: String, categorias: List<String>, lancamentos: List<Lancamento>, cor: Color) {
    val nomes = (categorias + lancamentos.map { it.categoria }).distinct()
    LinhaTabela(listOf(titulo, "Total no ano"), cabecalho = true, pesos = listOf(2.2f, 1f))
    nomes.forEach { c ->
        val total = lancamentos.filter { it.categoria == c }.sumOf { it.centavos }
        LinhaTabela(
            listOf(c, if (total != 0L) moeda(total) else "–"),
            cores = listOf(Color.Black, if (total != 0L) cor else Cinza),
            pesos = listOf(2.2f, 1f),
        )
        HorizontalDivider(color = Color(0xFFEEEEEE))
    }
}

@Composable
private fun LinhaTabela(
    textos: List<String>,
    cabecalho: Boolean = false,
    negrito: Boolean = false,
    destaque: Boolean = false,
    cores: List<Color>? = null,
    pesos: List<Float>? = null,
) {
    val fundo = when {
        cabecalho -> Color(0xFFE8ECF4)
        negrito -> Color(0xFFF1F4FA)
        destaque -> Color(0xFFFFF8E1)
        else -> Color.White
    }
    Row(
        Modifier.fillMaxWidth().background(fundo).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        textos.forEachIndexed { i, t ->
            Text(
                t,
                modifier = Modifier.weight(pesos?.getOrNull(i) ?: if (i == 0) 1.1f else 1f),
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                fontWeight = if (cabecalho || negrito) FontWeight.Bold else FontWeight.Normal,
                color = if (cabecalho) Azul else cores?.getOrNull(i) ?: Color.Black,
                style = if (cabecalho) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
