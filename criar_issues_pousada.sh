#!/usr/bin/env bash
# =============================================================================
# Cria labels, milestones e issues do projeto "API da Pousada" (GRUAPIM Tema 9)
#
# Pré-requisitos:
#   1. GitHub CLI instalado:  https://cli.github.com
#   2. Login feito:           gh auth login
#   3. Rodar DENTRO da pasta do repositório (ou use: export GH_REPO=usuario/repo)
#   4. Repositório SEM issues anteriores, para os números (#1, #2...) baterem
#      com as dependências escritas nas descrições.
#
# Uso:
#   chmod +x criar_issues_pousada.sh
#   ./criar_issues_pousada.sh
# =============================================================================
set -euo pipefail

# --------------------------- 1. LABELS ---------------------------------------
echo ">> Criando labels..."
create_label() { gh label create "$1" --color "$2" --description "$3" --force >/dev/null; }

# Tipo
create_label "feature"       "0E8A16" "Nova funcionalidade"
create_label "bug"           "D73A4A" "Algo não está funcionando"
create_label "docs"          "0075CA" "Documentação"
create_label "tests"         "FBCA04" "Testes automatizados"
create_label "setup"         "5319E7" "Configuração do projeto"
create_label "regra-de-negocio" "B60205" "Regra de negócio crítica"
# Área
create_label "auth"          "1D76DB" "Autenticação e autorização"
create_label "cadastros"     "C5DEF5" "Tipos de quarto, quartos e temporadas"
create_label "disponibilidade" "BFD4F2" "Consulta de disponibilidade e preço"
create_label "reservas"      "F9D0C4" "Reservas, sinal, cancelamento"
create_label "estadia"       "FEF2C0" "Check-in, consumos e check-out"
create_label "relatorios"    "D4C5F9" "Relatórios da recepção"
# Requisitos funcionais
for n in 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do
  create_label "RF$n" "EDEDED" "Requisito funcional $n"
done

# --------------------------- 2. MILESTONES -----------------------------------
echo ">> Criando milestones..."
create_milestone() {
  gh api repos/:owner/:repo/milestones -f title="$1" -f description="$2" >/dev/null
}
create_milestone "0 - Setup"                      "Estrutura do projeto, banco, seed e erros"
create_milestone "1 - Autenticação"               "RF01, RF02 e autorização por perfil"
create_milestone "2 - Cadastros"                  "RF03 e RF04"
create_milestone "3 - Disponibilidade"            "RF05 e regras de cálculo"
create_milestone "4 - Reservas"                   "RF06 a RF09"
create_milestone "5 - Estadia"                    "RF10 a RF13"
create_milestone "6 - Relatórios"                 "RF14 a RF16"
create_milestone "7 - Regras gerais e qualidade"  "Segurança, transações, testes e docs"

# --------------------------- 3. FUNÇÃO AUXILIAR ------------------------------
# Uso: new_issue "Título" "label1,label2" "Milestone" <<'EOF' ...corpo... EOF
new_issue() {
  local title="$1" labels="$2" milestone="$3"
  local body
  body="$(cat)"
  gh issue create --title "$title" --label "$labels" --milestone "$milestone" --body "$body"
  sleep 1   # evita limite de requisições
}

echo ">> Criando issues..."

# =============================================================================
# MILESTONE 0 - SETUP
# =============================================================================

# ---- #1
new_issue "Configurar projeto e repositório" "setup,feature" "0 - Setup" <<'EOF'
## Contexto
Base do projeto para a dupla trabalhar em paralelo.

## O que fazer
- [ ] Criar a estrutura inicial do projeto (framework escolhido)
- [ ] Criar `.gitignore` e `.env.example`
- [ ] Configurar variáveis de ambiente (porta, banco, segredo do JWT)
- [ ] Criar README inicial com instruções para rodar
- [ ] Proteger a branch `main` (PR obrigatório)

## Critérios de aceite
- [ ] O projeto sobe localmente com um único comando
- [ ] Nenhum segredo está versionado no repositório
- [ ] README explica como rodar

## Perfil com acesso
N/A

## Dependências
Nenhuma
EOF

# ---- #2
new_issue "Modelagem do banco de dados" "setup,docs" "0 - Setup" <<'EOF'
## Contexto
O domínio separa **tipo de quarto** (o que o hóspede reserva) de **quarto** (unidade física atribuída no check-in).

## O que fazer
- [ ] Modelar: Usuário (perfil hóspede/recepção), TipoQuarto, Quarto, Temporada, Reserva, Consumo, Conta/Pagamento
- [ ] Definir status do quarto: LIVRE, OCUPADO, EM_LIMPEZA, EM_MANUTENCAO
- [ ] Definir status da reserva: PENDENTE, CONFIRMADA, CANCELADA, EXPIRADA, NAO_COMPARECEU, EM_ESTADIA, FINALIZADA
- [ ] Criar migrations/scripts SQL
- [ ] Salvar o diagrama ER em `docs/der.png`

## Regras de negócio
- CPF e e-mail únicos no usuário
- Número do quarto único
- Reserva aponta para o **tipo**; o quarto só é preenchido no check-in

## Critérios de aceite
- [ ] Migrations rodam do zero sem erro
- [ ] DER publicado em `docs/`

## Dependências
Depende de #1
EOF

# ---- #3
new_issue "Carga inicial (seed) com conta da recepção" "setup,feature,auth" "0 - Setup" <<'EOF'
## Contexto
O enunciado diz que a conta da recepção "já vem criada na carga inicial".

## O que fazer
- [ ] Criar seed que insere o usuário de perfil RECEPCAO (senha com hash)
- [ ] Ler e-mail/senha iniciais de variáveis de ambiente
- [ ] Seed idempotente (não duplica se rodar duas vezes)

## Critérios de aceite
- [ ] Ao subir o sistema pela primeira vez existe um usuário de recepção
- [ ] Rodar o seed novamente não cria duplicado

## Dependências
Depende de #2
EOF

# ---- #4
new_issue "Tratamento global de erros e padrão de respostas" "setup,feature" "0 - Setup" <<'EOF'
## Contexto
O front-end é de outro time, então as respostas de erro precisam ser previsíveis.

## O que fazer
- [ ] Definir formato único de erro (ex.: `{ status, mensagem, detalhes }`)
- [ ] Mapear: 400/422 validação, 401 não autenticado, 403 sem permissão, 404 não encontrado, 409 conflito

## Critérios de aceite
- [ ] Todo erro segue o mesmo formato
- [ ] Nenhum erro vaza stack trace ao cliente

## Dependências
Depende de #1
EOF

# =============================================================================
# MILESTONE 1 - AUTENTICAÇÃO
# =============================================================================

# ---- #5
new_issue "RF01 - Cadastro de hóspede" "feature,auth,RF01" "1 - Autenticação" <<'EOF'
## Contexto
O hóspede se cadastra pela própria API. (RF01)

## O que fazer
- [ ] Endpoint `POST /auth/cadastro` (ou `/hospedes`)
- [ ] Campos: nome, CPF, e-mail, telefone, senha
- [ ] Validar CPF (formato) e e-mail
- [ ] Gravar senha com hash (bcrypt/argon2)
- [ ] Perfil fixo HOSPEDE (o cliente não escolhe o perfil)

## Regras de negócio
- CPF único e e-mail único
- Senha com no mínimo 8 caracteres
- **A resposta nunca devolve a senha**

## Critérios de aceite
- [ ] Cadastro válido retorna 201 sem o campo senha
- [ ] CPF ou e-mail duplicado retorna 409
- [ ] Senha com menos de 8 caracteres retorna 400/422
- [ ] Senha armazenada não está em texto puro

## Perfil com acesso
Público

## Dependências
Depende de #2, #4
EOF

# ---- #6
new_issue "RF02 - Login com JWT" "feature,auth,RF02" "1 - Autenticação" <<'EOF'
## Contexto
Login com e-mail e senha devolvendo um token JWT. (RF02)

## O que fazer
- [ ] Endpoint `POST /auth/login`
- [ ] Gerar JWT com identificação do usuário e o perfil
- [ ] Definir expiração do token

## Regras de negócio
- Credenciais inválidas **não indicam qual campo está errado** (mesma mensagem e mesmo status para e-mail inexistente e senha errada)

## Critérios de aceite
- [ ] Login válido retorna 200 com token; o token contém id e perfil
- [ ] E-mail inexistente e senha errada retornam resposta idêntica (401)
- [ ] Token expirado ou adulterado é rejeitado

## Perfil com acesso
Público

## Dependências
Depende de #5
EOF

# ---- #7
new_issue "Autorização por perfil (hóspede x recepção)" "feature,auth" "1 - Autenticação" <<'EOF'
## Contexto
O sistema tem permissões diferentes para cada perfil.

## O que fazer
- [ ] Middleware/filtro que valida o JWT
- [ ] Mecanismo de restrição por perfil (ex.: `@Roles("RECEPCAO")`)
- [ ] Tabela de permissões por endpoint documentada no README

## Critérios de aceite
- [ ] Sem token retorna 401
- [ ] Hóspede acessando endpoint da recepção retorna 403
- [ ] Recepção acessa seus endpoints normalmente

## Dependências
Depende de #6
EOF

# =============================================================================
# MILESTONE 2 - CADASTROS
# =============================================================================

# ---- #8
new_issue "RF03 - CRUD de tipos de quarto" "feature,cadastros,RF03" "2 - Cadastros" <<'EOF'
## Contexto
A recepção cadastra tipos de quarto (ex.: *duplo com vista para o mar*). (RF03)

## O que fazer
- [ ] `POST`, `GET` (lista e por id), `PUT`, `DELETE` em `/tipos-quarto`
- [ ] Campos: nome, capacidade, diária base
- [ ] Validar capacidade > 0 e diária base > 0
- [ ] Impedir exclusão de tipo com quartos ou reservas vinculados

## Critérios de aceite
- [ ] Recepção cria, edita e remove tipos
- [ ] Hóspede não altera tipos (403)
- [ ] Valores inválidos retornam 400/422

## Perfil com acesso
Recepção (escrita). Leitura pode ser pública para o site de reservas.

## Dependências
Depende de #7
EOF

# ---- #9
new_issue "RF03 - CRUD de quartos" "feature,cadastros,RF03" "2 - Cadastros" <<'EOF'
## Contexto
Quarto é a unidade física, com número, que a recepção atribui no check-in. (RF03)

## O que fazer
- [ ] `POST`, `GET`, `PUT`, `DELETE` em `/quartos`
- [ ] Campos: número (único) e tipo
- [ ] Status: LIVRE, OCUPADO, EM_LIMPEZA, EM_MANUTENCAO
- [ ] Endpoint para a recepção colocar/tirar o quarto de manutenção

## Regras de negócio
- Número único
- Quarto em manutenção não conta na disponibilidade do tipo
- Quarto ocupado não pode ser excluído

## Critérios de aceite
- [ ] Número duplicado retorna 409
- [ ] Tipo inexistente retorna 404/422
- [ ] Só a recepção altera quartos

## Dependências
Depende de #8
EOF

# ---- #10
new_issue "RF04 - CRUD de temporadas" "feature,cadastros,regra-de-negocio,RF04" "2 - Cadastros" <<'EOF'
## Contexto
A recepção cadastra temporadas com multiplicador da diária (ex.: 1,5 na alta). (RF04)

## O que fazer
- [ ] `POST`, `GET`, `PUT`, `DELETE` em `/temporadas`
- [ ] Campos: nome, data início, data fim, multiplicador
- [ ] Validar fim >= início e multiplicador > 0
- [ ] Validar sobreposição na criação **e na edição**

## Regras de negócio
- Temporadas **não se sobrepõem**
- Fora de qualquer temporada vale a diária base (multiplicador 1)
- Definir como identificar "alta temporada" (ex.: campo `alta` ou multiplicador > 1) e documentar

## Critérios de aceite
- [ ] Temporada sobreposta retorna 409
- [ ] Temporadas adjacentes (fim num dia, início no seguinte) são aceitas
- [ ] Edição que gere sobreposição é rejeitada

## Dependências
Depende de #7
EOF

# =============================================================================
# MILESTONE 3 - DISPONIBILIDADE
# =============================================================================

# ---- #11
new_issue "Regra de cálculo de disponibilidade por noite" "feature,disponibilidade,regra-de-negocio,tests" "3 - Disponibilidade" <<'EOF'
## Contexto
Disponibilidade de um tipo em uma noite = quartos do tipo **fora de manutenção** − reservas que ocupam aquela noite.

## O que fazer
- [ ] Criar serviço `calcularDisponibilidade(tipo, noite)`
- [ ] Definir quais status de reserva ocupam a noite (PENDENTE e CONFIRMADA ocupam; CANCELADA, EXPIRADA e NAO_COMPARECEU não)
- [ ] A noite de saída (check-out) **não** é ocupada
- [ ] Testes unitários dos casos de borda

## Critérios de aceite
- [ ] Quarto em manutenção reduz a disponibilidade
- [ ] Reserva cancelada/expirada libera a vaga
- [ ] Reserva que termina no dia X não bloqueia a noite de X

## Dependências
Depende de #9
EOF

# ---- #12
new_issue "Regra de cálculo do valor da estadia" "feature,disponibilidade,regra-de-negocio,tests" "3 - Disponibilidade" <<'EOF'
## Contexto
O valor é calculado **noite a noite**, com a tarifa da temporada daquela noite.

## O que fazer
- [ ] Criar serviço `calcularValor(tipo, entrada, saida)`
- [ ] Para cada noite: diária base × multiplicador da temporada (ou 1 se fora de temporada)
- [ ] Usar tipo decimal/monetário (nunca float)
- [ ] Identificar se alguma noite é de alta temporada (para a regra de mínimo de 2 diárias)

## Critérios de aceite
- [ ] Estadia dentro de uma temporada: valor correto
- [ ] Estadia que atravessa duas temporadas: cada noite com sua tarifa
- [ ] Estadia sem temporada: diária base
- [ ] Sem erro de arredondamento

## Dependências
Depende de #10
EOF

# ---- #13
new_issue "RF05 - Endpoint de consulta de disponibilidade" "feature,disponibilidade,RF05" "3 - Disponibilidade" <<'EOF'
## Contexto
Qualquer usuário consulta, para um período e nº de hóspedes, quais tipos têm quarto disponível em **todas** as noites e o valor total. (RF05)

## O que fazer
- [ ] `GET /disponibilidade?entrada=&saida=&hospedes=`
- [ ] Filtrar tipos com capacidade >= nº de hóspedes
- [ ] Retornar só tipos com vaga em todas as noites do período
- [ ] Retornar o valor total de cada tipo
- [ ] Validar datas (saída > entrada, entrada não no passado)

## Critérios de aceite
- [ ] Tipo sem vaga em uma única noite não aparece
- [ ] Tipo com capacidade menor que o nº de hóspedes não aparece
- [ ] Endpoint acessível a hóspede, recepção e (se decidirem) sem login
- [ ] Datas inválidas retornam 400

## Perfil com acesso
Qualquer usuário

## Dependências
Depende de #11, #12
EOF

# =============================================================================
# MILESTONE 4 - RESERVAS
# =============================================================================

# ---- #14
new_issue "RF06 - Criar reserva" "feature,reservas,regra-de-negocio,RF06" "4 - Reservas" <<'EOF'
## Contexto
O hóspede reserva um **tipo** de quarto para um período. (RF06)

## O que fazer
- [ ] `POST /reservas`
- [ ] Validar capacidade do tipo x nº de hóspedes
- [ ] Validar disponibilidade em cada noite
- [ ] Calcular valor noite a noite
- [ ] Aplicar estadia mínima de 2 diárias em alta temporada
- [ ] Salvar com status PENDENTE e registrar a data/hora de criação

## Regras de negócio
- Em alta temporada, estadia mínima = 2 diárias
- A reserva nasce **pendente**
- Hóspede reserva em seu próprio nome (id vem do token)

## Critérios de aceite
- [ ] Reserva válida retorna 201 com valor total e status PENDENTE
- [ ] Acima da capacidade retorna 400/422
- [ ] Noite sem vaga retorna 409
- [ ] 1 diária em alta temporada é rejeitada
- [ ] Recepção não reserva por este endpoint (ou regra documentada)

## Perfil com acesso
Hóspede

## Dependências
Depende de #8, #10, #13
EOF

# ---- #15
new_issue "Controle de concorrência na reserva (anti-overbooking)" "feature,reservas,regra-de-negocio,tests" "4 - Reservas" <<'EOF'
## Contexto
O problema original da pousada: dois hóspedes chegaram para o mesmo quarto. A API não pode permitir isso.

## O que fazer
- [ ] Executar checagem de disponibilidade + criação da reserva em **uma transação**
- [ ] Usar lock (pessimista) ou nível de isolamento adequado para o tipo/noites
- [ ] Teste simulando duas reservas simultâneas para a última vaga

## Critérios de aceite
- [ ] Com 1 vaga restante e 2 requisições simultâneas, só uma reserva é criada
- [ ] A outra recebe 409

## Dependências
Depende de #14
EOF

# ---- #16
new_issue "RF07 - Registrar sinal de 30%" "feature,reservas,regra-de-negocio,RF07" "4 - Reservas" <<'EOF'
## Contexto
A reserva nasce pendente e é confirmada quando a recepção registra o sinal de 30%. (RF07)

## O que fazer
- [ ] `POST /reservas/{id}/sinal`
- [ ] Calcular o sinal = 30% do valor total
- [ ] Registrar o pagamento e mudar status PENDENTE -> CONFIRMADA

## Critérios de aceite
- [ ] Sinal registrado confirma a reserva
- [ ] Reserva que não está pendente (cancelada, expirada, já confirmada) retorna 409
- [ ] Valor do sinal = 30% do total, sem erro de arredondamento
- [ ] Hóspede não registra sinal (403)

## Perfil com acesso
Recepção

## Dependências
Depende de #14
EOF

# ---- #17
new_issue "RF07 - Processar reservas pendentes expiradas" "feature,reservas,regra-de-negocio,RF07" "4 - Reservas" <<'EOF'
## Contexto
Reserva pendente há mais de 24 horas expira **quando a recepção processa as expiradas**. (RF07)

## O que fazer
- [ ] `POST /reservas/processar-expiradas`
- [ ] Buscar PENDENTES com criação há mais de 24h e marcar como EXPIRADA
- [ ] Retornar quantas foram expiradas

## Critérios de aceite
- [ ] Pendente com mais de 24h vira EXPIRADA
- [ ] Pendente com menos de 24h permanece
- [ ] Reservas confirmadas não são afetadas
- [ ] Vaga volta a ficar disponível após a expiração

## Perfil com acesso
Recepção

## Dependências
Depende de #14
EOF

# ---- #18
new_issue "RF08 - Cancelar reserva" "feature,reservas,regra-de-negocio,RF08" "4 - Reservas" <<'EOF'
## Contexto
Até 7 dias antes da entrada, o sinal é devolvido integralmente; depois disso, fica retido. (RF08)

## O que fazer
- [ ] `POST /reservas/{id}/cancelar`
- [ ] Calcular a diferença entre hoje e a data de entrada
- [ ] >= 7 dias de antecedência: registrar devolução integral do sinal
- [ ] < 7 dias: sinal retido
- [ ] Mudar status para CANCELADA

## Regras de negócio
- Hóspede só cancela as próprias reservas
- Reserva pendente (sem sinal) cancela sem movimentação financeira
- Não cancelar reserva já finalizada, em estadia, cancelada ou expirada

## Critérios de aceite
- [ ] Teste na fronteira: exatamente 7 dias, 6 dias e 8 dias antes
- [ ] Hóspede cancelando reserva de outro retorna 403/404
- [ ] Vaga liberada após o cancelamento

## Perfil com acesso
Hóspede (dono da reserva) e recepção

## Dependências
Depende de #16
EOF

# ---- #19
new_issue "RF09 - Check-in com atribuição de quarto" "feature,estadia,regra-de-negocio,RF09" "4 - Reservas" <<'EOF'
## Contexto
Na data de entrada, a recepção atribui um quarto livre do tipo reservado. (RF09)

## O que fazer
- [ ] `POST /reservas/{id}/checkin`
- [ ] Validar que a reserva está CONFIRMADA e que hoje é a data de entrada
- [ ] Escolher/receber um quarto LIVRE do tipo reservado
- [ ] Vincular quarto à reserva, quarto -> OCUPADO, reserva -> EM_ESTADIA
- [ ] Tudo na **mesma transação**

## Critérios de aceite
- [ ] Check-in válido altera reserva e quarto juntos
- [ ] Reserva não confirmada retorna 409
- [ ] Fora da data de entrada retorna 409/422
- [ ] Quarto de outro tipo ou não livre é rejeitado
- [ ] Falha no meio não deixa dados parciais

## Perfil com acesso
Recepção

## Dependências
Depende de #9, #16
EOF

# ---- #20
new_issue "RF09 - Marcar não comparecimento" "feature,reservas,regra-de-negocio,RF09" "4 - Reservas" <<'EOF'
## Contexto
Reserva confirmada sem check-in até o dia seguinte é marcada como não comparecimento, com o sinal retido. (RF09)

## O que fazer
- [ ] `POST /reservas/processar-nao-comparecimentos`
- [ ] Buscar CONFIRMADAS sem check-in cuja data de entrada já passou do dia seguinte
- [ ] Marcar como NAO_COMPARECEU e manter o sinal retido

## Critérios de aceite
- [ ] Confirmada sem check-in após o prazo vira NAO_COMPARECEU
- [ ] Reserva com check-in feito não é afetada
- [ ] Sinal não é devolvido
- [ ] Vaga liberada

## Perfil com acesso
Recepção

## Dependências
Depende de #19
EOF

# =============================================================================
# MILESTONE 5 - ESTADIA
# =============================================================================

# ---- #21
new_issue "RF10 - Lançar consumos na estadia" "feature,estadia,RF10" "5 - Estadia" <<'EOF'
## Contexto
A recepção lança consumos (frigobar, restaurante, lavanderia) na estadia de um quarto ocupado. (RF10)

## O que fazer
- [ ] `POST /reservas/{id}/consumos`
- [ ] Campos: categoria (FRIGOBAR, RESTAURANTE, LAVANDERIA), descrição, quantidade, valor unitário
- [ ] Aceitar lançamento só se a reserva estiver EM_ESTADIA (quarto ocupado)

## Critérios de aceite
- [ ] Lançamento em estadia ativa retorna 201
- [ ] Lançamento em reserva sem check-in ou já finalizada retorna 409
- [ ] Valores negativos ou zero são rejeitados
- [ ] Consumo aparece no extrato

## Perfil com acesso
Recepção

## Dependências
Depende de #19
EOF

# ---- #22
new_issue "RF11 - Check-out e fechamento da conta" "feature,estadia,regra-de-negocio,RF11" "5 - Estadia" <<'EOF'
## Contexto
A recepção fecha a conta (diárias + consumos − sinal) e registra o pagamento; o quarto vai para limpeza. (RF11)

## O que fazer
- [ ] `POST /reservas/{id}/checkout`
- [ ] Calcular: total das diárias + total dos consumos − sinal pago
- [ ] Registrar o pagamento
- [ ] Reserva -> FINALIZADA, quarto -> EM_LIMPEZA
- [ ] Tudo na **mesma transação**

## Critérios de aceite
- [ ] Valor final correto com e sem consumos
- [ ] Quarto fica EM_LIMPEZA após o check-out
- [ ] Falha no meio não deixa dados parciais
- [ ] Reserva que não está EM_ESTADIA retorna 409

## Perfil com acesso
Recepção

## Dependências
Depende de #21
EOF

# ---- #23
new_issue "RF11 - Saída antecipada (multa de 1 diária)" "feature,estadia,regra-de-negocio,RF11" "5 - Estadia" <<'EOF'
## Contexto
Saída antecipada cobra as diárias usadas mais uma diária de multa. (RF11)

## O que fazer
- [ ] Detectar check-out antes da data de saída prevista
- [ ] Cobrar apenas as diárias efetivamente usadas (com a tarifa de cada noite) + 1 diária de multa
- [ ] Definir e documentar qual valor é a "diária de multa" (ex.: tarifa da última noite usada)
- [ ] Mostrar a multa discriminada no extrato

## Critérios de aceite
- [ ] Saída no dia previsto: sem multa
- [ ] Saída antecipada: diárias usadas + multa
- [ ] Teste com estadia que atravessa temporadas

## Perfil com acesso
Recepção

## Dependências
Depende de #22
EOF

# ---- #24
new_issue "RF12 - Liberar quarto após a limpeza" "feature,estadia,regra-de-negocio,RF12" "5 - Estadia" <<'EOF'
## Contexto
A recepção libera o quarto depois da limpeza ou o coloca em manutenção (só um quarto livre ou em limpeza). (RF12)

## O que fazer
- [ ] `POST /quartos/{id}/liberar` -> LIVRE
- [ ] `POST /quartos/{id}/manutencao` -> EM_MANUTENCAO
- [ ] Validar o status de origem (apenas EM_LIMPEZA pode ser liberado)

## Critérios de aceite
- [ ] Quarto EM_LIMPEZA vira LIVRE ou EM_MANUTENCAO
- [ ] Quarto OCUPADO não pode ser liberado (409)
- [ ] Quarto em manutenção deixa de contar na disponibilidade

## Perfil com acesso
Recepção

## Dependências
Depende de #22
EOF

# ---- #25
new_issue "RF13 - Hóspede consulta suas reservas e extrato" "feature,estadia,auth,RF13" "5 - Estadia" <<'EOF'
## Contexto
O hóspede vê as próprias reservas e o extrato da conta durante a estadia. (RF13)

## O que fazer
- [ ] `GET /minhas-reservas`
- [ ] `GET /reservas/{id}/extrato` (diárias, consumos, sinal, saldo)
- [ ] Filtrar sempre pelo id do usuário do token

## Critérios de aceite
- [ ] Hóspede vê apenas as suas reservas
- [ ] Acessar o extrato de outro hóspede retorna 403/404
- [ ] Extrato mostra consumos lançados até o momento

## Perfil com acesso
Hóspede (e recepção para qualquer reserva)

## Dependências
Depende de #21
EOF

# =============================================================================
# MILESTONE 6 - RELATÓRIOS
# =============================================================================

# ---- #26
new_issue "RF14 - Relatório de taxa de ocupação por período" "feature,relatorios,RF14" "6 - Relatórios" <<'EOF'
## Contexto
Relatório da recepção com a taxa de ocupação por período. (RF14)

## O que fazer
- [ ] `GET /relatorios/ocupacao?inicio=&fim=`
- [ ] Taxa = noites ocupadas / (quartos disponíveis x nº de noites)
- [ ] Documentar se quartos em manutenção entram no denominador

## Critérios de aceite
- [ ] Resultado confere com um cenário de teste calculado à mão
- [ ] Período inválido retorna 400
- [ ] Só a recepção acessa

## Perfil com acesso
Recepção

## Dependências
Depende de #19
EOF

# ---- #27
new_issue "RF15 - Relatório de receita por tipo de quarto" "feature,relatorios,RF15" "6 - Relatórios" <<'EOF'
## Contexto
Receita por tipo de quarto, separando diárias e consumos. (RF15)

## O que fazer
- [ ] `GET /relatorios/receita?inicio=&fim=`
- [ ] Agrupar por tipo de quarto
- [ ] Separar: receita de diárias, receita de consumos, total
- [ ] Decidir e documentar se a multa e o sinal retido entram na receita

## Critérios de aceite
- [ ] Soma por tipo confere com as contas fechadas
- [ ] Só a recepção acessa

## Perfil com acesso
Recepção

## Dependências
Depende de #22
EOF

# ---- #28
new_issue "RF16 - Relatório de entradas e saídas previstas do dia" "feature,relatorios,RF16" "6 - Relatórios" <<'EOF'
## Contexto
Lista de entradas e saídas previstas para o dia. (RF16)

## O que fazer
- [ ] `GET /relatorios/movimento-dia?data=` (padrão: hoje)
- [ ] Entradas: reservas CONFIRMADAS com entrada na data
- [ ] Saídas: reservas EM_ESTADIA com saída na data
- [ ] Incluir nome do hóspede, tipo de quarto e quarto (nas saídas)

## Critérios de aceite
- [ ] Entradas e saídas aparecem em listas separadas
- [ ] Canceladas/expiradas não aparecem
- [ ] Só a recepção acessa

## Perfil com acesso
Recepção

## Dependências
Depende de #19
EOF

# =============================================================================
# MILESTONE 7 - REGRAS GERAIS E QUALIDADE
# =============================================================================

# ---- #29
new_issue "Hóspede só acessa as próprias reservas e contas" "feature,auth,regra-de-negocio,tests" "7 - Regras gerais e qualidade" <<'EOF'
## Contexto
Regra geral: o hóspede só acessa as próprias reservas e contas.

## O que fazer
- [ ] Revisar TODOS os endpoints que recebem id de reserva/conta
- [ ] Garantir verificação de propriedade pelo id do token
- [ ] Testes de acesso cruzado entre dois hóspedes

## Critérios de aceite
- [ ] Hóspede A não vê, não cancela e não consulta extrato do hóspede B
- [ ] Resposta não revela se o recurso existe (usar 404 ou 403 de forma consistente)

## Dependências
Depende de #18, #25
EOF

# ---- #30
new_issue "Atomicidade de reserva, check-in e check-out (transações)" "feature,regra-de-negocio,tests" "7 - Regras gerais e qualidade" <<'EOF'
## Contexto
Regra geral: check-in e check-out alteram reserva, quarto e conta na mesma operação; ou tudo é gravado, ou nada é.

## O que fazer
- [ ] Revisar check-in (#19) e check-out (#22) para uso de transação
- [ ] Teste que força falha no meio da operação e verifica rollback

## Critérios de aceite
- [ ] Falha simulada deixa reserva, quarto e conta inalterados

## Dependências
Depende de #19, #22
EOF

# ---- #31
new_issue "Testes automatizados das regras de negócio" "tests,regra-de-negocio" "7 - Regras gerais e qualidade" <<'EOF'
## Contexto
As regras do enunciado são o coração da avaliação e precisam estar cobertas por testes.

## O que fazer
- [ ] Testes unitários: disponibilidade, cálculo de valor, sinal 30%, cancelamento (7 dias), multa, mínimo de 2 diárias
- [ ] Testes de integração dos fluxos: cadastro -> login -> reservar -> sinal -> check-in -> consumo -> check-out
- [ ] Configurar execução dos testes no GitHub Actions (opcional)

## Critérios de aceite
- [ ] Todas as regras com número/prazo no enunciado têm teste de fronteira
- [ ] Comando de testes documentado no README

## Dependências
Depende de #22
EOF

# ---- #32
new_issue "Documentação da API (Swagger/OpenAPI + coleção)" "docs" "7 - Regras gerais e qualidade" <<'EOF'
## Contexto
Os front-ends são de outro time, então a documentação da API é parte da entrega.

## O que fazer
- [ ] Gerar/escrever a documentação OpenAPI (Swagger UI)
- [ ] Documentar autenticação (Bearer JWT), perfis exigidos e códigos de erro de cada endpoint
- [ ] Exportar coleção Postman/Insomnia em `docs/`

## Critérios de aceite
- [ ] Todos os endpoints aparecem no Swagger com exemplos de request/response
- [ ] Coleção importável e funcional

## Dependências
Depende de #28
EOF

# ---- #33
new_issue "README final e divisão de trabalho da dupla" "docs" "7 - Regras gerais e qualidade" <<'EOF'
## Contexto
Entrega final do repositório.

## O que fazer
- [ ] README com: descrição, tecnologias, como rodar, usuário inicial, testes, estrutura e decisões de projeto
- [ ] Registrar as decisões tomadas onde o enunciado era ambíguo (ex.: definição de alta temporada, multa, taxa de ocupação)
- [ ] Descrever a divisão de trabalho entre os integrantes
- [ ] Revisar o repositório (sem segredos, sem código morto)

## Critérios de aceite
- [ ] Alguém de fora consegue rodar o projeto só com o README

## Dependências
Depende de #32
EOF

echo ""
echo "Pronto! 33 issues criadas. Confira em: gh issue list --limit 50"
