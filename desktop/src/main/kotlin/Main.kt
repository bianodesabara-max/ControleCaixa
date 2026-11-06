package caixa

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

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

fun pastaDeDados(): File = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "ControleCaixa")

@Composable
fun BarraRolagem(estado: LazyListState, modifier: Modifier) {
    VerticalScrollbar(adapter = rememberScrollbarAdapter(estado), modifier = modifier)
}

/** Abre a janela "Salvar como" do Windows e grava a planilha. */
@Composable
fun rememberExportador(avisar: (String) -> Unit): (String, String) -> Unit {
    val avisarAtual by rememberUpdatedState(avisar)
    return { nome, conteudo ->
        try {
            val janela = FileDialog(null as Frame?, "Salvar planilha", FileDialog.SAVE)
            janela.file = nome
            janela.isVisible = true
            val escolhido = janela.file
            if (escolhido != null) {
                var arquivo = File(janela.directory, escolhido)
                if (!arquivo.name.lowercase().endsWith(".csv")) arquivo = File(arquivo.parentFile, arquivo.name + ".csv")
                arquivo.writeText(conteudo, Charsets.UTF_8)
                avisarAtual("Planilha salva em ${arquivo.absolutePath}")
            }
        } catch (e: Exception) {
            avisarAtual("Não foi possível salvar a planilha: ${e.message}")
        }
    }
}
