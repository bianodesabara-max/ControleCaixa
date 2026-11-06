package caixa

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val Azul = Color(0xFF1F3864)
val Verde = Color(0xFF1B7F3B)
val Vermelho = Color(0xFFB3261E)
val Cinza = Color(0xFF5F6368)
val Fundo = Color(0xFFF3F5F9)

/** true quando a tela é estreita (celular): muda a navegação e o jeito de mostrar as listas. */
val LocalCompacto = staticCompositionLocalOf { false }

enum class Tela(val titulo: String, val curto: String, val icone: ImageVector) {
    LANCAMENTOS("Lançamentos", "Caixa", Icons.Filled.Payments),
    RESUMO("Resumo Mensal", "Resumo", Icons.Filled.BarChart),
    CONFERENCIA("Conferência", "Conferir", Icons.Filled.Calculate),
    CATEGORIAS("Categorias", "Categorias", Icons.Filled.Category),
}

@Composable
fun Tema(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Azul,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCE4F2),
            onPrimaryContainer = Azul,
            secondary = Color(0xFF3A5A8C),
            background = Fundo,
            surface = Color.White,
            error = Vermelho,
        ),
        content = conteudo,
    )
}

@Composable
fun App() {
    var dados by remember { mutableStateOf(Armazenamento.carregar()) }
    var tela by remember { mutableStateOf(Tela.LANCAMENTOS) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val avisar: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    val atualizar: ((Dados) -> Dados) -> Unit = { mudanca ->
        val novo = mudanca(dados)
        dados = novo
        try {
            Armazenamento.salvar(novo)
        } catch (e: Exception) {
            avisar("Erro ao salvar os dados: ${e.message}")
        }
    }
    val exportar = rememberExportador(avisar)

    LaunchedEffect(Unit) {
        Armazenamento.avisoAoCarregar?.let { snackbar.showSnackbar(it, withDismissAction = true) }
    }

    Tema {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compacto = maxWidth < 720.dp
            CompositionLocalProvider(LocalCompacto provides compacto) {
                Scaffold(
                    containerColor = Fundo,
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        if (compacto) {
                            NavigationBar(containerColor = Azul) {
                                Tela.entries.forEach { t ->
                                    NavigationBarItem(
                                        selected = tela == t,
                                        onClick = { tela = t },
                                        icon = { Icon(t.icone, contentDescription = null) },
                                        label = { Text(t.curto, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Azul,
                                            selectedTextColor = Color.White,
                                            indicatorColor = Color.White,
                                            unselectedIconColor = Color.White.copy(alpha = 0.75f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.75f),
                                        ),
                                    )
                                }
                            }
                        }
                    },
                ) { margens ->
                    Row(Modifier.fillMaxSize().padding(margens)) {
                        if (!compacto) {
                            NavigationRail(
                                containerColor = Azul,
                                contentColor = Color.White,
                                modifier = Modifier.fillMaxHeight().width(112.dp),
                                header = {
                                    Spacer(Modifier.height(12.dp))
                                    MarcaApp()
                                    Spacer(Modifier.height(16.dp))
                                },
                            ) {
                                Tela.entries.forEach { t ->
                                    NavigationRailItem(
                                        selected = tela == t,
                                        onClick = { tela = t },
                                        icon = { Icon(t.icone, contentDescription = null) },
                                        label = { Text(t.titulo, textAlign = TextAlign.Center) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = Azul,
                                            selectedTextColor = Color.White,
                                            indicatorColor = Color.White,
                                            unselectedIconColor = Color.White.copy(alpha = 0.75f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.75f),
                                        ),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                        Box(
                            Modifier.weight(1f).fillMaxHeight().background(Fundo)
                                .padding(if (compacto) 12.dp else 24.dp)
                        ) {
                            when (tela) {
                                Tela.LANCAMENTOS -> TelaLancamentos(dados, atualizar, avisar, exportar)
                                Tela.RESUMO -> TelaResumo(dados)
                                Tela.CONFERENCIA -> TelaConferencia(dados, atualizar, avisar)
                                Tela.CATEGORIAS -> TelaCategorias(dados, atualizar, avisar)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarcaApp() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(48.dp, 32.dp).background(Color(0xFF2EA05A), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AttachMoney, null, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text("Caixa", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// ---------- Componentes comuns ----------

@Composable
fun Titulo(texto: String) {
    Text(
        texto,
        style = if (LocalCompacto.current) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Azul,
    )
}

@Composable
fun CartaoValor(titulo: String, valor: String, cor: Color, modifier: Modifier = Modifier, detalhe: String? = null) {
    val compacto = LocalCompacto.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(if (compacto) 12.dp else 16.dp)) {
            Text(
                titulo, color = Cinza, maxLines = 2,
                style = if (compacto) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                valor, fontWeight = FontWeight.Bold, color = cor, maxLines = 1, softWrap = false,
                style = if (compacto) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
            )
            if (detalhe != null) {
                Text(detalhe, style = MaterialTheme.typography.bodySmall, color = Cinza)
            }
        }
    }
}

@Composable
fun CartaoBranco(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = conteudo,
    )
}

/** Botão que abre uma lista para escolher uma opção. */
@Composable
fun Seletor(
    rotulo: String,
    texto: String,
    opcoes: List<String>,
    onEscolher: (Int) -> Unit,
    modifier: Modifier = Modifier,
    erro: Boolean = false,
) {
    var aberto by remember { mutableStateOf(false) }
    Column(modifier) {
        if (rotulo.isNotEmpty()) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = if (erro) Vermelho else Cinza)
            Spacer(Modifier.height(2.dp))
        }
        Box {
            OutlinedButton(
                onClick = { aberto = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraSmall,
                border = BorderStroke(1.dp, if (erro) Vermelho else Color(0xFF79747E)),
            ) {
                Text(texto, modifier = Modifier.weight(1f), color = Color.Black)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Cinza)
            }
            DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                opcoes.forEachIndexed { i, op ->
                    DropdownMenuItem(text = { Text(op) }, onClick = { aberto = false; onEscolher(i) })
                }
            }
        }
    }
}

@Composable
fun DialogoConfirmar(titulo: String, mensagem: String, botao: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensagem) },
        confirmButton = {
            Button(onClick = onConfirmar, colors = ButtonDefaults.buttonColors(containerColor = Vermelho)) { Text(botao) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

// ---------- Exportar ----------

private fun celula(s: String): String =
    if (s.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

/** Monta o conteúdo da planilha (.csv, separado por ponto e vírgula, que o Excel em português abre direto). */
fun gerarCsv(dados: Dados): String {
    val calc = Calculo(dados)
    val sb = StringBuilder("﻿")
    sb.append("Data;Tipo;Descrição;Categoria;Entrada (R$);Saída (R$);Saldo (R$);Responsável;Observação\r\n")
    sb.append(dataBR(dados.dataSaldoInicial)).append(";;Saldo inicial;;;;")
        .append(numeroPlanilha(dados.saldoInicialCentavos)).append(";;\r\n")
    for (l in calc.ordenados) {
        sb.append(
            listOf(
                dataBR(l.dataLocal),
                if (l.entrada) "Entrada" else "Saída",
                celula(l.descricao),
                celula(l.categoria),
                if (l.entrada) numeroPlanilha(l.centavos) else "",
                if (!l.entrada) numeroPlanilha(l.centavos) else "",
                numeroPlanilha(calc.saldoApos[l.id] ?: 0L),
                celula(l.responsavel),
                celula(l.observacao),
            ).joinToString(";")
        ).append("\r\n")
    }
    return sb.toString()
}
