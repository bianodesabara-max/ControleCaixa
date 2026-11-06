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
import androidx.compose.ui.unit.dp

@Composable
fun TelaCategorias(dados: Dados, atualizar: ((Dados) -> Dados) -> Unit, avisar: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Titulo("Categorias")
        Text(
            "Ao renomear, os lançamentos já feitos com essa categoria também são atualizados.",
            color = Cinza,
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ListaCategorias(
                titulo = "Categorias de entrada", cor = Verde, ehEntrada = true,
                dados = dados, atualizar = atualizar, avisar = avisar,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ListaCategorias(
                titulo = "Categorias de saída", cor = Vermelho, ehEntrada = false,
                dados = dados, atualizar = atualizar, avisar = avisar,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

private fun Dados.categorias(ehEntrada: Boolean) = if (ehEntrada) categoriasEntrada else categoriasSaida

private fun Dados.comCategorias(ehEntrada: Boolean, lista: List<String>) =
    if (ehEntrada) copy(categoriasEntrada = lista) else copy(categoriasSaida = lista)

@Composable
private fun ListaCategorias(
    titulo: String,
    cor: Color,
    ehEntrada: Boolean,
    dados: Dados,
    atualizar: ((Dados) -> Dados) -> Unit,
    avisar: (String) -> Unit,
    modifier: Modifier,
) {
    val lista = dados.categorias(ehEntrada)
    var nova by remember { mutableStateOf("") }
    var renomeando by remember { mutableStateOf<String?>(null) }
    var excluindo by remember { mutableStateOf<String?>(null) }

    fun existe(nome: String) = lista.any { it.equals(nome.trim(), ignoreCase = true) }

    CartaoBranco(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cor)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nova, onValueChange = { nova = it },
                    label = { Text("Nova categoria") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val nome = nova.trim()
                    when {
                        nome.isEmpty() -> avisar("Digite o nome da categoria.")
                        existe(nome) -> avisar("Essa categoria já existe.")
                        else -> {
                            atualizar { d -> d.comCategorias(ehEntrada, d.categorias(ehEntrada) + nome) }
                            nova = ""
                        }
                    }
                }) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar")
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                lista.forEach { c ->
                    val usos = dados.lancamentos.count { it.entrada == ehEntrada && it.categoria == c }
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c)
                            Text(
                                if (usos == 0) "sem lançamentos" else "$usos lançamento(s)",
                                style = MaterialTheme.typography.bodySmall, color = Cinza,
                            )
                        }
                        IconButton(onClick = { renomeando = c }) { Icon(Icons.Filled.Edit, "Renomear", tint = Azul) }
                        IconButton(onClick = { excluindo = c }) { Icon(Icons.Filled.Delete, "Excluir", tint = Vermelho) }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
        }
    }

    renomeando?.let { antigo ->
        var texto by remember(antigo) { mutableStateOf(antigo) }
        AlertDialog(
            onDismissRequest = { renomeando = null },
            title = { Text("Renomear categoria") },
            text = {
                OutlinedTextField(
                    value = texto, onValueChange = { texto = it },
                    label = { Text("Nome") }, singleLine = true, modifier = Modifier.width(420.dp),
                )
            },
            confirmButton = {
                Button(onClick = {
                    val novo = texto.trim()
                    when {
                        novo.isEmpty() -> avisar("Digite o nome da categoria.")
                        novo != antigo && existe(novo) -> avisar("Essa categoria já existe.")
                        else -> {
                            atualizar { d ->
                                d.comCategorias(ehEntrada, d.categorias(ehEntrada).map { if (it == antigo) novo else it })
                                    .copy(lancamentos = d.lancamentos.map {
                                        if (it.entrada == ehEntrada && it.categoria == antigo) it.copy(categoria = novo) else it
                                    })
                            }
                            renomeando = null
                        }
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { renomeando = null }) { Text("Cancelar") } },
        )
    }

    excluindo?.let { nome ->
        DialogoConfirmar(
            titulo = "Excluir categoria?",
            mensagem = "\"$nome\" sai da lista de opções. Os lançamentos já feitos com ela continuam como estão.",
            botao = "Excluir",
            onConfirmar = {
                atualizar { d -> d.comCategorias(ehEntrada, d.categorias(ehEntrada).filter { it != nome }) }
                excluindo = null
            },
            onCancelar = { excluindo = null },
        )
    }
}
