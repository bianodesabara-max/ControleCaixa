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

private fun situacao(diferenca: Long): Pair<String, Color> = when {
    diferenca == 0L -> "CAIXA CONFERE" to Verde
    diferenca > 0L -> "SOBRA NO CAIXA" to Color(0xFFB26A00)
    else -> "FALTA NO CAIXA" to Vermelho
}

@Composable
fun TelaConferencia(dados: Dados, atualizar: ((Dados) -> Dados) -> Unit, avisar: (String) -> Unit) {
    val calc = remember(dados) { Calculo(dados) }
    var dataTxt by remember { mutableStateOf(dataBR(LocalDate.now())) }
    var conferidoPor by remember { mutableStateOf(dados.ultimoResponsavel) }
    val quantidades = remember { mutableStateListOf(*Array(CEDULAS_E_MOEDAS.size) { "" }) }
    var excluindo by remember { mutableStateOf<Conferencia?>(null) }

    val data = lerData(dataTxt)
    val contado = CEDULAS_E_MOEDAS.indices.sumOf { i -> CEDULAS_E_MOEDAS[i].second * (quantidades[i].toLongOrNull() ?: 0L) }
    val sistema = calc.saldoAte(data ?: LocalDate.now())
    val diferenca = contado - sistema
    val (textoSituacao, corSituacao) = situacao(diferenca)

    Column(Modifier.fillMaxSize()) {
        Titulo("Conferência de Caixa")
        Text("Conte as notas e moedas e digite só a quantidade de cada uma.", color = Cinza)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // ---- Contagem ----
            CartaoBranco(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dataTxt, onValueChange = { dataTxt = it },
                            label = { Text("Data da contagem") }, isError = data == null,
                            singleLine = true, modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = conferidoPor, onValueChange = { conferidoPor = it },
                            label = { Text("Conferido por") },
                            singleLine = true, modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFFE8ECF4)).padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        listOf("Cédula / Moeda", "Valor", "Quantidade", "Total").forEachIndexed { i, t ->
                            Text(
                                t, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Azul,
                                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    CEDULAS_E_MOEDAS.forEachIndexed { i, (tipo, valor) ->
                        val qtd = quantidades[i].toLongOrNull() ?: 0L
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tipo, Modifier.weight(1f))
                            Text(moeda(valor), Modifier.weight(1f), textAlign = TextAlign.End)
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                OutlinedTextField(
                                    value = quantidades[i],
                                    onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() }) quantidades[i] = v },
                                    placeholder = { Text("0") },
                                    singleLine = true,
                                    modifier = Modifier.width(110.dp),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                                )
                            }
                            Text(
                                if (qtd > 0) moeda(valor * qtd) else "–", Modifier.weight(1f),
                                textAlign = TextAlign.End, fontWeight = if (qtd > 0) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            // ---- Resultado e histórico ----
            Column(Modifier.weight(1f).fillMaxHeight()) {
                CartaoBranco(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinhaResultado("Total contado (dinheiro físico)", moeda(contado), Color.Black)
                        LinhaResultado(
                            "Saldo pelo sistema" + (data?.let { " até ${dataBR(it)}" } ?: ""),
                            moeda(sistema), Verde,
                        )
                        HorizontalDivider()
                        LinhaResultado("Diferença (contado – sistema)", moeda(diferenca), corSituacao)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.fillMaxWidth().background(corSituacao.copy(alpha = 0.12f), MaterialTheme.shapes.medium)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                textoSituacao, color = corSituacao, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (data == null) {
                                        avisar("Digite uma data válida (dd/mm/aaaa).")
                                    } else {
                                        atualizar { d ->
                                            val nova = Conferencia(
                                                id = (d.conferencias.maxOfOrNull { it.id } ?: 0L) + 1,
                                                data = data.toString(),
                                                conferidoPor = conferidoPor.trim(),
                                                contadoCentavos = contado,
                                                sistemaCentavos = sistema,
                                                quantidades = quantidades.map { it.toIntOrNull() ?: 0 },
                                            )
                                            d.copy(conferencias = d.conferencias + nova)
                                        }
                                        avisar("Conferência salva no histórico.")
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.Save, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Salvar conferência")
                            }
                            OutlinedButton(onClick = { for (i in quantidades.indices) quantidades[i] = "" }) {
                                Text("Limpar contagem")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Histórico de conferências", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Azul)
                Spacer(Modifier.height(8.dp))
                CartaoBranco(Modifier.fillMaxSize()) {
                    if (dados.conferencias.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhuma conferência salva ainda.", color = Cinza)
                        }
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            dados.conferencias.sortedWith(compareByDescending<Conferencia> { it.data }.thenByDescending { it.id })
                                .forEach { c ->
                                    val (txt, cor) = situacao(c.diferenca)
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                dataBR(c.data) + if (c.conferidoPor.isNotBlank()) " · ${c.conferidoPor}" else "",
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                "Contado ${moeda(c.contadoCentavos)} · Sistema ${moeda(c.sistemaCentavos)}",
                                                style = MaterialTheme.typography.bodySmall, color = Cinza,
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(txt, color = cor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                            if (c.diferenca != 0L) Text(moeda(c.diferenca), color = cor, style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { excluindo = c }) {
                                            Icon(Icons.Filled.Delete, "Excluir", tint = Cinza)
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFEEEEEE))
                                }
                        }
                    }
                }
            }
        }
    }

    excluindo?.let { c ->
        DialogoConfirmar(
            titulo = "Excluir conferência?",
            mensagem = "Conferência de ${dataBR(c.data)} (contado ${moeda(c.contadoCentavos)}).",
            botao = "Excluir",
            onConfirmar = {
                atualizar { d -> d.copy(conferencias = d.conferencias.filter { it.id != c.id }) }
                excluindo = null
            },
            onCancelar = { excluindo = null },
        )
    }
}

@Composable
private fun LinhaResultado(rotulo: String, valor: String, cor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, Modifier.weight(1f), color = Cinza)
        Text(valor, fontWeight = FontWeight.Bold, color = cor, style = MaterialTheme.typography.titleLarge)
    }
}
