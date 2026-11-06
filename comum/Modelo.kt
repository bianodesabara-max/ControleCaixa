package caixa

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import kotlin.math.abs

val CATEGORIAS_ENTRADA = listOf(
    "Aluguéis",
    "Mensalidade do Plano de Saúde",
    "Diferença do Plano de Saúde",
    "Mensalidade do Dependente do Plano de Saúde",
    "Valor da Coparticipação do Plano de Saúde",
    "Pagamento de Nota Promissória",
    "Outras entradas",
)

val CATEGORIAS_SAIDA = listOf(
    "Fornecedores",
    "Despesas gerais",
    "Transporte",
    "Alimentação",
    "Material de limpeza",
    "Salários / Diárias",
    "Retirada / Sangria",
    "Outras saídas",
)

/** Cédulas e moedas em centavos, na ordem em que aparecem na conferência. */
val CEDULAS_E_MOEDAS: List<Pair<String, Long>> = listOf(
    "Cédula" to 20000L, "Cédula" to 10000L, "Cédula" to 5000L, "Cédula" to 2000L,
    "Cédula" to 1000L, "Cédula" to 500L, "Cédula" to 200L,
    "Moeda" to 100L, "Moeda" to 50L, "Moeda" to 25L, "Moeda" to 10L, "Moeda" to 5L,
)

@Serializable
data class Lancamento(
    val id: Long,
    val data: String, // aaaa-mm-dd
    val descricao: String,
    val categoria: String,
    val entrada: Boolean,
    val centavos: Long,
    val responsavel: String = "",
    val observacao: String = "",
) {
    val dataLocal: LocalDate get() = LocalDate.parse(data)
    val comSinal: Long get() = if (entrada) centavos else -centavos
}

@Serializable
data class Conferencia(
    val id: Long,
    val data: String,
    val conferidoPor: String,
    val contadoCentavos: Long,
    val sistemaCentavos: Long,
    val quantidades: List<Int> = emptyList(),
) {
    val diferenca: Long get() = contadoCentavos - sistemaCentavos
}

@Serializable
data class Dados(
    val saldoInicialCentavos: Long = 0,
    val dataSaldoInicial: String = LocalDate.now().toString(),
    val categoriasEntrada: List<String> = CATEGORIAS_ENTRADA,
    val categoriasSaida: List<String> = CATEGORIAS_SAIDA,
    val lancamentos: List<Lancamento> = emptyList(),
    val conferencias: List<Conferencia> = emptyList(),
    val ultimoResponsavel: String = "",
)

/** Cálculos derivados dos dados (saldo de cada linha, saldo atual etc.). */
class Calculo(val dados: Dados) {
    val ordenados: List<Lancamento> =
        dados.lancamentos.sortedWith(compareBy<Lancamento>({ it.data }, { it.id }))

    val saldoApos: Map<Long, Long> = run {
        var s = dados.saldoInicialCentavos
        val m = HashMap<Long, Long>()
        for (l in ordenados) {
            s += l.comSinal
            m[l.id] = s
        }
        m
    }

    val saldoAtual: Long = dados.saldoInicialCentavos + dados.lancamentos.sumOf { it.comSinal }

    fun saldoAte(dia: LocalDate): Long {
        val limite = dia.toString()
        return dados.saldoInicialCentavos + dados.lancamentos.filter { it.data <= limite }.sumOf { it.comSinal }
    }

    fun doMes(mes: YearMonth): List<Lancamento> = ordenados.filter { YearMonth.from(it.dataLocal) == mes }
}

object Armazenamento {
    val pasta: File by lazy { pastaDeDados() }
    private val arquivo by lazy { File(pasta, "dados.json") }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    /** Mensagem a mostrar quando o arquivo de dados não pôde ser lido. */
    var avisoAoCarregar: String? = null
        private set

    fun carregar(): Dados {
        if (!arquivo.exists()) return Dados()
        return try {
            json.decodeFromString(Dados.serializer(), arquivo.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            val carimbo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val copia = File(pasta, "dados-com-erro-$carimbo.json")
            runCatching { arquivo.copyTo(copia, overwrite = true) }
            avisoAoCarregar = "Não foi possível ler os dados salvos. Uma cópia foi guardada em ${copia.absolutePath}."
            Dados()
        }
    }

    fun salvar(dados: Dados) {
        pasta.mkdirs()
        val tmp = File(pasta, "dados.tmp")
        tmp.writeText(json.encodeToString(Dados.serializer(), dados), Charsets.UTF_8)
        if (arquivo.exists()) runCatching { arquivo.copyTo(File(pasta, "dados.bak"), overwrite = true) }
        try {
            Files.move(tmp.toPath(), arquivo.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: Exception) {
            Files.move(tmp.toPath(), arquivo.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

// ---------- Formatos ----------

private val formatoData = DateTimeFormatter.ofPattern("dd/MM/uuuu")
private val leituraData = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)

val NOMES_MESES = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
)

fun dataBR(d: LocalDate): String = d.format(formatoData)
fun dataBR(iso: String): String = runCatching { dataBR(LocalDate.parse(iso)) }.getOrDefault(iso)

fun lerData(texto: String): LocalDate? {
    val t = texto.trim()
    // Aceita também só os números: 06112026 = 06/11/2026
    val comBarras = if (Regex("""\d{8}""").matches(t)) "${t.substring(0, 2)}/${t.substring(2, 4)}/${t.substring(4)}" else t
    return runCatching { LocalDate.parse(comBarras, leituraData) }.getOrNull()
}

fun nomeMes(m: YearMonth): String = "${NOMES_MESES[m.monthValue - 1]}/${m.year}"

/** 123456 -> "R$ 1.234,56" */
fun moeda(centavos: Long): String {
    val a = abs(centavos)
    val reais = (a / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    val cent = (a % 100).toString().padStart(2, '0')
    return (if (centavos < 0) "-" else "") + "R$ " + reais + "," + cent
}

/** 123456 -> "1234,56" (formato que o Excel em português reconhece como número) */
fun numeroPlanilha(centavos: Long): String {
    val a = abs(centavos)
    return (if (centavos < 0) "-" else "") + (a / 100) + "," + (a % 100).toString().padStart(2, '0')
}

/** Texto para editar um valor: 123456 -> "1.234,56" */
fun valorEditavel(centavos: Long): String = moeda(centavos).replace("R$ ", "")

/** Lê "1.234,56", "1234,56", "1234.56", "1234" ou "R$ 10" e devolve centavos. */
fun lerValor(texto: String): Long? {
    var t = texto.trim().replace("R$", "").replace(" ", "").replace(" ", "")
    if (t.isEmpty()) return null
    val negativo = t.startsWith("-")
    if (negativo) t = t.substring(1)
    t = when {
        t.contains(',') -> t.replace(".", "").replace(',', '.')
        Regex("""\d{1,3}(\.\d{3})+""").matches(t) -> t.replace(".", "")
        else -> t
    }
    if (!Regex("""\d+(\.\d{1,2})?""").matches(t)) return null
    return runCatching {
        val c = BigDecimal(t).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        if (negativo) -c else c
    }.getOrNull()
}
