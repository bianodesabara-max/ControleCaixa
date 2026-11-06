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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.LocalDate

val Azul = Color(0xFF1F3864)
val Verde = Color(0xFF1B7F3B)
val Vermelho = Color(0xFFB3261E)
val Cinza = Color(0xFF5F6368)
val Fundo = Color(0xFFF3F5F9)

fun main() = application {
    val estado = rememberWindowState(
        width = 1280.dp,
        height = 820.dp,
        position = WindowPosition(Alignment.Center),
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Controle de Caixa",
        state = estado,
        icon = painterResource("icone.png"),
    ) {
        App()
    }
}

enum class Tela(val titulo: String, val icone: ImageVector) {
    LANCAMENTOS("Lançamentos", Icons.Filled.Payments),
    RESUMO("Resumo Mensal", Icons.Filled.BarChart),
    CONFERENCIA("Conferência", Icons.Filled.Calculate),
    CATEGORIAS("Categorias", Icons.Filled.Category),
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

    LaunchedEffect(Unit) {
        Armazenamento.avisoAoCarregar?.let { snackbar.showSnackbar(it, withDismissAction = true) }
    }

    Tema {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { margens ->
            Row(Modifier.fillMaxSize().padding(margens)) {
                NavigationRail(
                    containerColor = Azul,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxHeight().width(112.dp),
                    header = {
                        Spacer(Modifier.height(12.dp))
                        Image(painterResource("icone.png"), null, Modifier.size(48.dp))
                        Text("Caixa", color = Color.White, fontWeight = FontWeight.Bold)
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
                Box(
                    Modifier.weight(1f).fillMaxHeight().background(Fundo).padding(24.dp)
                ) {
                    when (tela) {
                        Tela.LANCAMENTOS -> TelaLancamentos(dados, atualizar, avisar)
                        Tela.RESUMO -> TelaResumo(dados)
                        Tela.CONFERENCIA -> TelaConferencia(dados, atualizar, avisar)
                        Tela.CATEGORIAS -> TelaCategorias(dados, atualizar, avisar)
                    }
                }
            }
        }
    }
}

// ---------- Componentes comuns ----------

@Composable
fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Azul)
}

@Composable
fun CartaoValor(titulo: String, valor: String, cor: Color, modifier: Modifier = Modifier, detalhe: String? = null) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelLarge, color = Cinza)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = cor)
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

/** Abre a janela "Salvar como" e grava os lançamentos num arquivo que abre no Excel. */
fun exportarParaExcel(dados: Dados): File? {
    val janela = FileDialog(null as Frame?, "Salvar planilha", FileDialog.SAVE)
    janela.file = "controle-de-caixa-${LocalDate.now()}.csv"
    janela.isVisible = true
    val nome = janela.file ?: return null
    var arquivo = File(janela.directory, nome)
    if (!arquivo.name.lowercase().endsWith(".csv")) arquivo = File(arquivo.parentFile, arquivo.name + ".csv")

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
    arquivo.writeText(sb.toString(), Charsets.UTF_8)
    return arquivo
}
