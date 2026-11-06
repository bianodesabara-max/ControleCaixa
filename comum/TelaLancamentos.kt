package caixa

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

// Larguras relativas das colunas da tabela
private val LARGURA_DATA = 104.dp
private const val C_DESC = 1.9f
private const val C_CAT = 1.7f
private const val C_VALOR = 1.3f
private const val C_SALDO = 1.4f
private const val C_RESP = 1.1f
private val LARGURA_ACOES = 96.dp

@Composable
fun TelaLancamentos(
    dados: Dados,
    atualizar: ((Dados) -> Dados) -> Unit,
    avisar: (String) -> Unit,
    exportar: (String, String) -> Unit,
) {
    val compacto = LocalCompacto.current
    val calc = remember(dados) { Calculo(dados) }
    var novoEhEntrada by remember { mutableStateOf<Boolean?>(null) }
    var editando by remember { mutableStateOf<Lancamento?>(null) }
    var excluindo by remember { mutableStateOf<Lancamento?>(null) }
    var editarSaldoInicial by remember { mutableStateOf(false) }
    var periodo by remember { mutableStateOf<YearMonth?>(YearMonth.now()) }
    var busca by remember { mutableStateOf("") }

    val meses = remember(dados) {
        (dados.lancamentos.map { YearMonth.from(it.dataLocal) } + YearMonth.now()).distinct().sortedDescending()
    }
    val filtrados = remember(dados, periodo, busca) {
        calc.ordenados.asReversed().filter { l ->
            (periodo == null || YearMonth.from(l.dataLocal) == periodo) &&
                (busca.isBlank() || listOf(l.descricao, l.categoria, l.responsavel, l.observacao)
                    .any { it.contains(busca.trim(), ignoreCase = true) })
        }
    }
    val entradasPeriodo = filtrados.filter { it.entrada }.sumOf { it.centavos }
    val saidasPeriodo = filtrados.filter { !it.entrada }.sumOf { it.centavos }
    val sufixo = periodo?.let { " em ${nomeMes(it)}" } ?: " (todos)"
    val acaoExportar: () -> Unit = { exportar("controle-de-caixa-${LocalDate.now()}.csv", gerarCsv(dados)) }
    val textoVazio = if (dados.lancamentos.isEmpty()) "Nenhum lançamento ainda. Use \"Nova entrada\" ou \"Nova saída\"."
    else "Nenhum lançamento neste período ou busca."

    val cartaoSaldoInicial: @Composable (Modifier) -> Unit = { m ->
        CartaoValor(
            "Saldo inicial", moeda(dados.saldoInicialCentavos), Azul,
            m.clickable { editarSaldoInicial = true },
            detalhe = "em ${dataBR(dados.dataSaldoInicial)} · " + if (compacto) "toque para alterar" else "clique para alterar",
        )
    }
    val cartaoSaldoAtual: @Composable (Modifier) -> Unit = { m ->
        CartaoValor(
            "Saldo atual do caixa", moeda(calc.saldoAtual),
            if (calc.saldoAtual < 0) Vermelho else Azul, m,
            detalhe = "${dados.lancamentos.size} lançamento(s)",
        )
    }
    val seletorPeriodo: @Composable (Modifier) -> Unit = { m ->
        Seletor(
            rotulo = "Período",
            texto = periodo?.let { nomeMes(it) } ?: "Todos os lançamentos",
            opcoes = listOf("Todos os lançamentos") + meses.map { nomeMes(it) },
            onEscolher = { i -> periodo = if (i == 0) null else meses[i - 1] },
            modifier = m,
        )
    }
    val campoBusca: @Composable (Modifier) -> Unit = { m ->
        OutlinedTextField(
            value = busca,
            onValueChange = { busca = it },
            placeholder = { Text("Buscar lançamentos", maxLines = 1) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (busca.isNotEmpty()) IconButton(onClick = { busca = "" }) { Icon(Icons.Filled.Close, "Limpar") }
            },
            singleLine = true,
            modifier = m,
        )
    }

    if (compacto) {
        // ----- Celular: tudo numa lista só, que rola inteira -----
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Titulo("Lançamentos")
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = acaoExportar) {
                        Icon(Icons.Filled.FileDownload, "Exportar para Excel", tint = Azul)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoNovo(true, Modifier.weight(1f)) { novoEhEntrada = true }
                    BotaoNovo(false, Modifier.weight(1f)) { novoEhEntrada = false }
                }
            }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cartaoSaldoAtual(Modifier.weight(1f).fillMaxHeight())
                    cartaoSaldoInicial(Modifier.weight(1f).fillMaxHeight())
                }
            }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CartaoValor("Entradas$sufixo", moeda(entradasPeriodo), Verde, Modifier.weight(1f).fillMaxHeight())
                    CartaoValor("Saídas$sufixo", moeda(saidasPeriodo), Vermelho, Modifier.weight(1f).fillMaxHeight())
                }
            }
            item { seletorPeriodo(Modifier.fillMaxWidth()) }
            item { campoBusca(Modifier.fillMaxWidth()) }
            if (filtrados.isEmpty()) {
                item {
                    Text(textoVazio, color = Cinza, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
                }
            } else {
                items(filtrados, key = { it.id }) { l ->
                    ItemLancamentoCompacto(
                        l, calc.saldoApos[l.id] ?: 0L,
                        onEditar = { editando = l },
                        onExcluir = { excluindo = l },
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    } else {
        // ----- Computador: tabela -----
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Titulo("Lançamentos")
                Spacer(Modifier.weight(1f))
                BotaoNovo(true) { novoEhEntrada = true }
                Spacer(Modifier.width(8.dp))
                BotaoNovo(false) { novoEhEntrada = false }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = acaoExportar) {
                    Icon(Icons.Filled.FileDownload, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar para Excel")
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cartaoSaldoInicial(Modifier.weight(1f).fillMaxHeight())
                CartaoValor("Entradas$sufixo", moeda(entradasPeriodo), Verde, Modifier.weight(1f).fillMaxHeight())
                CartaoValor("Saídas$sufixo", moeda(saidasPeriodo), Vermelho, Modifier.weight(1f).fillMaxHeight())
                cartaoSaldoAtual(Modifier.weight(1f).fillMaxHeight())
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                seletorPeriodo(Modifier.width(260.dp))
                Spacer(Modifier.width(12.dp))
                campoBusca(Modifier.width(440.dp))
            }

            Spacer(Modifier.height(12.dp))
            CartaoBranco(Modifier.fillMaxSize()) {
                CabecalhoLancamentos()
                HorizontalDivider()
                if (filtrados.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(textoVazio, color = Cinza)
                    }
                } else {
                    val lista = rememberLazyListState()
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(state = lista, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
                            items(filtrados, key = { it.id }) { l ->
                                LinhaLancamento(
                                    l, calc.saldoApos[l.id] ?: 0L,
                                    onEditar = { editando = l },
                                    onExcluir = { excluindo = l },
                                )
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                            }
                        }
                        BarraRolagem(lista, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                    }
                }
            }
        }
    }

    // ---------- Janelas ----------
    val salvarLancamento: (Lancamento) -> Unit = { l ->
        atualizar { d ->
            val lista = if (l.id == 0L) {
                d.lancamentos + l.copy(id = (d.lancamentos.maxOfOrNull { it.id } ?: 0L) + 1)
            } else {
                d.lancamentos.map { if (it.id == l.id) l else it }
            }
            d.copy(lancamentos = lista, ultimoResponsavel = l.responsavel.ifBlank { d.ultimoResponsavel })
        }
    }

    novoEhEntrada?.let { ehEntrada ->
        DialogoLancamento(
            inicial = null, entradaPadrao = ehEntrada, dados = dados,
            onSalvar = { salvarLancamento(it); novoEhEntrada = null; avisar("Lançamento salvo.") },
            onCancelar = { novoEhEntrada = null },
        )
    }
    editando?.let { l ->
        DialogoLancamento(
            inicial = l, entradaPadrao = l.entrada, dados = dados,
            onSalvar = { salvarLancamento(it); editando = null; avisar("Lançamento alterado.") },
            onCancelar = { editando = null },
        )
    }
    excluindo?.let { l ->
        DialogoConfirmar(
            titulo = "Excluir lançamento?",
            mensagem = "${dataBR(l.dataLocal)} · ${l.descricao} · ${moeda(l.centavos)}\n\nEsta ação não pode ser desfeita.",
            botao = "Excluir",
            onConfirmar = {
                atualizar { d -> d.copy(lancamentos = d.lancamentos.filter { it.id != l.id }) }
                excluindo = null
                avisar("Lançamento excluído.")
            },
            onCancelar = { excluindo = null },
        )
    }
    if (editarSaldoInicial) {
        DialogoSaldoInicial(
            dados = dados,
            onSalvar = { valor, data ->
                atualizar { d -> d.copy(saldoInicialCentavos = valor, dataSaldoInicial = data.toString()) }
                editarSaldoInicial = false
            },
            onCancelar = { editarSaldoInicial = false },
        )
    }
}

@Composable
private fun BotaoNovo(entrada: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = if (entrada) Verde else Vermelho),
    ) {
        Icon(if (entrada) Icons.Filled.Add else Icons.Filled.Remove, null)
        Spacer(Modifier.width(6.dp))
        Text(if (entrada) "Nova entrada" else "Nova saída", maxLines = 1)
    }
}

@Composable
private fun ItemLancamentoCompacto(l: Lancamento, saldo: Long, onEditar: () -> Unit, onExcluir: () -> Unit) {
    CartaoBranco(Modifier.fillMaxWidth().clickable(onClick = onEditar)) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.descricao, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${dataBR(l.dataLocal)} · ${l.categoria}",
                    style = MaterialTheme.typography.bodySmall, color = Cinza,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (l.observacao.isNotBlank() || l.responsavel.isNotBlank()) {
                    Text(
                        listOf(l.responsavel, l.observacao).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = Cinza,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (l.entrada) "+ " else "− ") + moeda(l.centavos),
                    color = if (l.entrada) Verde else Vermelho, fontWeight = FontWeight.Bold,
                    maxLines = 1, softWrap = false,
                )
                Text(
                    "Saldo ${moeda(saldo)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (saldo < 0) Vermelho else Cinza, maxLines = 1, softWrap = false,
                )
            }
            IconButton(onClick = onExcluir) { Icon(Icons.Filled.Delete, "Excluir", tint = Cinza) }
        }
    }
}

@Composable
private fun CabecalhoLancamentos() {
    val estilo = MaterialTheme.typography.labelLarge
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFE8ECF4)).padding(horizontal = 12.dp, vertical = 10.dp).padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Data", Modifier.width(LARGURA_DATA), style = estilo, fontWeight = FontWeight.Bold, color = Azul)
        Text("Descrição", Modifier.weight(C_DESC), style = estilo, fontWeight = FontWeight.Bold, color = Azul)
        Text("Categoria", Modifier.weight(C_CAT), style = estilo, fontWeight = FontWeight.Bold, color = Azul)
        Text("Entrada", Modifier.weight(C_VALOR).padding(start = 8.dp), style = estilo, fontWeight = FontWeight.Bold, color = Azul, textAlign = TextAlign.End)
        Text("Saída", Modifier.weight(C_VALOR).padding(start = 8.dp), style = estilo, fontWeight = FontWeight.Bold, color = Azul, textAlign = TextAlign.End)
        Text("Saldo", Modifier.weight(C_SALDO).padding(start = 8.dp), style = estilo, fontWeight = FontWeight.Bold, color = Azul, textAlign = TextAlign.End)
        Spacer(Modifier.width(16.dp))
        Text("Responsável", Modifier.weight(C_RESP), style = estilo, fontWeight = FontWeight.Bold, color = Azul)
        Spacer(Modifier.width(LARGURA_ACOES))
    }
}

@Composable
private fun LinhaLancamento(l: Lancamento, saldo: Long, onEditar: () -> Unit, onExcluir: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(dataBR(l.dataLocal), Modifier.width(LARGURA_DATA), maxLines = 1, softWrap = false)
        Column(Modifier.weight(C_DESC).padding(end = 8.dp)) {
            Text(l.descricao, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (l.observacao.isNotBlank()) {
                Text(l.observacao, style = MaterialTheme.typography.bodySmall, color = Cinza, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(l.categoria, Modifier.weight(C_CAT).padding(end = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(if (l.entrada) moeda(l.centavos) else "", Modifier.weight(C_VALOR).padding(start = 8.dp), color = Verde, textAlign = TextAlign.End, maxLines = 1, softWrap = false)
        Text(if (!l.entrada) moeda(l.centavos) else "", Modifier.weight(C_VALOR).padding(start = 8.dp), color = Vermelho, textAlign = TextAlign.End, maxLines = 1, softWrap = false)
        Text(
            moeda(saldo), Modifier.weight(C_SALDO).padding(start = 8.dp), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
            color = if (saldo < 0) Vermelho else Color.Black,
        )
        Spacer(Modifier.width(16.dp))
        Text(l.responsavel, Modifier.weight(C_RESP), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.width(LARGURA_ACOES), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, "Editar", tint = Azul) }
            IconButton(onClick = onExcluir) { Icon(Icons.Filled.Delete, "Excluir", tint = Vermelho) }
        }
    }
}

@Composable
fun DialogoLancamento(
    inicial: Lancamento?,
    entradaPadrao: Boolean,
    dados: Dados,
    onSalvar: (Lancamento) -> Unit,
    onCancelar: () -> Unit,
) {
    var entrada by remember { mutableStateOf(inicial?.entrada ?: entradaPadrao) }
    var dataTxt by remember { mutableStateOf(dataBR(inicial?.dataLocal ?: LocalDate.now())) }
    var descricao by remember { mutableStateOf(inicial?.descricao ?: "") }
    var categoria by remember { mutableStateOf(inicial?.categoria ?: "") }
    var valorTxt by remember { mutableStateOf(inicial?.let { valorEditavel(it.centavos) } ?: "") }
    var responsavel by remember { mutableStateOf(inicial?.responsavel ?: dados.ultimoResponsavel) }
    var observacao by remember { mutableStateOf(inicial?.observacao ?: "") }
    var tentou by remember { mutableStateOf(false) }

    val categorias = if (entrada) dados.categoriasEntrada else dados.categoriasSaida
    val data = lerData(dataTxt)
    val valor = lerValor(valorTxt)
    val valorOk = valor != null && valor > 0
    val tudoOk = data != null && descricao.isNotBlank() && categoria.isNotBlank() && valorOk

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(
                when {
                    inicial != null -> "Editar lançamento"
                    entrada -> "Nova entrada de dinheiro"
                    else -> "Nova saída de dinheiro"
                }
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = entrada,
                        onClick = { if (!entrada) { entrada = true; categoria = "" } },
                        label = { Text("Entrada") },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Verde, selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                        ),
                    )
                    FilterChip(
                        selected = !entrada,
                        onClick = { if (entrada) { entrada = false; categoria = "" } },
                        label = { Text("Saída") },
                        leadingIcon = { Icon(Icons.Filled.Remove, null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Vermelho, selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                        ),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dataTxt, onValueChange = { dataTxt = it },
                        label = { Text("Data (dd/mm/aaaa)") },
                        isError = tentou && data == null,
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = valorTxt, onValueChange = { valorTxt = it },
                        label = { Text("Valor (R$)") }, placeholder = { Text("0,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = tentou && !valorOk,
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = descricao, onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    isError = tentou && descricao.isBlank(),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Seletor(
                    rotulo = "Categoria",
                    texto = categoria.ifBlank { "Escolha a categoria" },
                    opcoes = categorias,
                    onEscolher = { categoria = categorias[it] },
                    modifier = Modifier.fillMaxWidth(),
                    erro = tentou && categoria.isBlank(),
                )
                OutlinedTextField(
                    value = responsavel, onValueChange = { responsavel = it },
                    label = { Text("Responsável") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = observacao, onValueChange = { observacao = it },
                    label = { Text("Observação (opcional)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                )
                if (tentou && !tudoOk) {
                    Text("Confira os campos destacados em vermelho.", color = Vermelho)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                tentou = true
                if (tudoOk) {
                    onSalvar(
                        Lancamento(
                            id = inicial?.id ?: 0L,
                            data = data!!.toString(),
                            descricao = descricao.trim(),
                            categoria = categoria,
                            entrada = entrada,
                            centavos = valor!!,
                            responsavel = responsavel.trim(),
                            observacao = observacao.trim(),
                        )
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogoSaldoInicial(dados: Dados, onSalvar: (Long, LocalDate) -> Unit, onCancelar: () -> Unit) {
    var valorTxt by remember { mutableStateOf(valorEditavel(dados.saldoInicialCentavos)) }
    var dataTxt by remember { mutableStateOf(dataBR(dados.dataSaldoInicial)) }
    var tentou by remember { mutableStateOf(false) }
    val valor = lerValor(valorTxt)
    val data = lerData(dataTxt)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Saldo inicial do caixa") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dinheiro que já estava no caixa antes do primeiro lançamento.", color = Cinza)
                OutlinedTextField(
                    value = valorTxt, onValueChange = { valorTxt = it },
                    label = { Text("Valor (R$)") }, isError = tentou && valor == null,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dataTxt, onValueChange = { dataTxt = it },
                    label = { Text("Data (dd/mm/aaaa)") }, isError = tentou && data == null,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                tentou = true
                if (valor != null && data != null) onSalvar(valor, data)
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
