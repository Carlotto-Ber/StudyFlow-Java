STUDYFLOW - PROTÓTIPO COM ALTA VARIABILIDADE

1. O QUE É ALTA VARIABILIDADE

Alta variabilidade é a capacidade de criar versões diferentes do mesmo
software sem desenvolver tudo novamente. No StudyFlow, a base é reutilizada,
mas cada cliente pode receber módulos, cores, termos, regras de cálculo,
canais de lembrete e permissões diferentes.

2. COMO TESTAR O PROTÓTIPO

Abra HTML/dashboard.html. No topo da tela, altere o cliente e o perfil.
O menu será montado novamente conforme a configuração escolhida.

Clientes disponíveis:
- StudyFlow Individual: somente os recursos básicos.
- Colégio Horizonte: provas, avisos e administração de usuários.
- Universidade Aurora: sessões, desempenho e média ponderada.
- Instituto Técnico Atlas: todos os módulos e Laboratório Inteligente.

Perfis disponíveis:
- Estudante: consulta e organização dos próprios estudos.
- Professor: provas, avisos e acompanhamento das turmas.
- Administrador: usuários, módulos e personalização institucional.

3. AS 12 INTERFACES

01. dashboard.html       - painel e mapa de variação
02. disciplinas.html     - cadastro de disciplinas/matérias/unidades técnicas
03. tarefas.html         - organização de tarefas
04. calendario.html      - calendário pessoal ou institucional
05. sessoes.html         - sessões de estudo (opcional)
06. desempenho.html      - progresso e estratégia de média (opcional)
07. avisos.html          - mural com canais variáveis (opcional)
08. provas.html          - avaliações e notas (opcional)
09. usuarios.html        - gerenciamento exclusivo do administrador
10. personalizacao.html  - tema, termos e módulos por cliente
11. laboratorio.html     - função exclusiva do Instituto Técnico Atlas
12. arquitetura.html     - arquitetura, tecnologias e fontes pesquisadas

4. FUNÇÃO ESPECÍFICA DE UM CLIENTE

O Laboratório Inteligente foi criado somente para o Instituto Técnico Atlas.
Ele permite reservar laboratórios e consultar equipamentos. O módulo está em
uma página própria e possui a regra clienteExclusivo: atlas no arquivo
JS/config.js. Assim, ele não aparece no menu de outros clientes. Mesmo no
Instituto Atlas, apenas professor e administrador possuem acesso.

5. ARQUITETURA INDICADA

Foi escolhida uma Linha de Produto de Software com arquitetura modular.
A solução possui:

- Núcleo comum: autenticação, disciplinas, tarefas e calendário.
- Módulos opcionais: provas, avisos, sessões e desempenho.
- Módulo exclusivo: laboratório do Instituto Atlas.
- Configuração por cliente: define módulos, aparência, termos e regras.
- RBAC: controla as interfaces de estudante, professor e administrador.
- Strategy: separa as formas de calcular média ou conceito.
- Componentes reutilizáveis: cartões, tabelas, menu, formulários e avisos.

Tecnologias sugeridas para uma versão real:
- Linguagem: TypeScript.
- Interface: React e biblioteca própria de componentes.
- Back-end: Node.js, começando como monólito modular.
- SGBD: PostgreSQL.
- Separação de dados: identificador da instituição e Row-Level Security.
- Testes: testes do núcleo e testes adicionais para cada módulo ativado.

O monólito modular é uma escolha realista para o tamanho atual do StudyFlow.
Ele evita a complexidade inicial de vários microsserviços, mas mantém os
módulos separados. Caso o sistema cresça, módulos com grande demanda poderão
ser transformados em serviços independentes.

6. FONTES PESQUISADAS

- Software Engineering Institute - Software Product Lines Collection:
  https://www.sei.cmu.edu/library/software-product-lines-collection/
- React - componentes reutilizáveis:
  https://react.dev/learn/your-first-component
- TypeScript - JavaScript com tipos:
  https://www.typescriptlang.org/
- PostgreSQL - Row Security Policies:
  https://www.postgresql.org/docs/current/ddl-rowsecurity.html

7. ORGANIZAÇÃO DOS ARQUIVOS

Trabalho/
  HTML/  - 12 interfaces HTML
  CSS/   - estilos compartilhados
  JS/    - configuração da variabilidade e comportamento das telas
