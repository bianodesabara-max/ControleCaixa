# Controle de Caixa (Windows)

Aplicativo de Windows para controle de caixa em dinheiro, baseado na planilha de controle de caixa.

## Telas

- **Lançamentos**: entradas e saídas com data, descrição, categoria, responsável e observação; saldo de cada linha e saldo atual; filtro por mês e busca; exportação para Excel (.csv).
- **Resumo Mensal**: entradas, saídas, resultado e saldo no fim de cada mês, e totais do ano por categoria.
- **Conferência de Caixa**: contagem de cédulas e moedas, comparação com o saldo do sistema (confere / sobra / falta) e histórico das conferências.
- **Categorias**: categorias de entrada e de saída, com inclusão, renomeação e exclusão.

## Onde ficam os dados

`%APPDATA%\ControleCaixa\dados.json` (com uma cópia de segurança automática em `dados.bak`).

## Como gerar o instalador

O workflow **Gerar instalador Windows** (`.github/workflows/gerar-instalador.yml`) roda a cada envio de código
e gera o artefato **ControleCaixa-instalador** com o arquivo `.exe`.
