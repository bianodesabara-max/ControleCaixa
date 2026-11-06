package caixa

import android.content.Context
import android.graphics.Color as CorAndroid
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Contexto do app, usado para saber onde gravar os dados. */
lateinit var contextoApp: Context

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        contextoApp = applicationContext
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(CorAndroid.TRANSPARENT, CorAndroid.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(CorAndroid.TRANSPARENT),
        )
        setContent { App() }
    }
}

fun pastaDeDados(): File = contextoApp.filesDir

/** No celular a rolagem é feita com o dedo, sem barra lateral. */
@Composable
fun BarraRolagem(estado: LazyListState, modifier: Modifier) {
}

/** Abre a tela "Salvar em" do Android e grava a planilha onde a pessoa escolher. */
@Composable
fun rememberExportador(avisar: (String) -> Unit): (String, String) -> Unit {
    val contexto = LocalContext.current
    var pendente by remember { mutableStateOf<String?>(null) }
    val avisarAtual by rememberUpdatedState(avisar)
    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val conteudo = pendente
        pendente = null
        if (uri != null && conteudo != null) {
            try {
                contexto.contentResolver.openOutputStream(uri)?.use { it.write(conteudo.toByteArray(Charsets.UTF_8)) }
                avisarAtual("Planilha salva.")
            } catch (e: Exception) {
                avisarAtual("Não foi possível salvar a planilha: ${e.message}")
            }
        }
    }
    return { nome, conteudo ->
        pendente = conteudo
        seletor.launch(nome)
    }
}
