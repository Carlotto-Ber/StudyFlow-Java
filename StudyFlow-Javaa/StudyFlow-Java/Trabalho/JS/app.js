(async function () {
  const config = window.STUDYFLOW_CONFIG;
  const api = window.StudyFlowAPI;
  const U = window.StudyFlowUtil;
  const page = document.body.dataset.page;
  const module = config.modulos[page];
  const app = document.getElementById("app");

  function erroInicial(mensagem) {
    return `<div class="denied card"><div class="symbol">⚠</div><h3>Não foi possível carregar o StudyFlow</h3>
      <div class="alert">${U.esc(mensagem)}</div>
      <p>Confirme se o backend está rodando e se o banco foi populado (classe <code>Seed</code>).<br>Abra o sistema por <code>http://localhost:5000</code>, e não pelo arquivo HTML.</p></div>`;
  }

  app.innerHTML = '<div class="boot">Carregando StudyFlow...</div>';

  // ---- as instituições vêm do banco de dados
  let instituicoes;
  try {
    instituicoes = await api.get("/instituicoes");
  } catch (erro) {
    app.innerHTML = erroInicial(erro.message);
    return;
  }
  if (!instituicoes.length) {
    app.innerHTML = erroInicial("Nenhuma instituição cadastrada no banco de dados.");
    return;
  }
  config.clientes = {};
  instituicoes.forEach((i) => {
    config.clientes[i.codigo] = {
      id: i.id, nome: i.nome, cor: i.cor, corEscura: i.cor_escura, modulos: i.modulos, editaveis: i.editaveis,
      termos: i.termos, media: i.media, metodoMedia: i.metodo_media, lembretes: i.lembretes, canais: i.canais
    };
  });

  const savedClient = localStorage.getItem("studyflow_cliente");
  const savedRole = localStorage.getItem("studyflow_perfil");
  const clientKey = config.clientes[savedClient] ? savedClient : instituicoes[0].codigo;
  const roleKey = config.perfis[savedRole] ? savedRole : "estudante";
  const client = config.clientes[clientKey];

  // ---- usuário atual: quem está "logado" nesta demonstração (a lista vem do banco)
  let usuarios = [];
  try {
    usuarios = await api.get(`/usuarios?instituicao=${client.id}&tipo=${roleKey}`);
  } catch (erro) {
    usuarios = [];
  }
  const chaveUsuario = `studyflow_usuario_${clientKey}_${roleKey}`;
  const usuarioSalvo = Number(localStorage.getItem(chaveUsuario));
  const usuario = usuarios.find((u) => u.id === usuarioSalvo) || usuarios[0] || null;

  document.documentElement.style.setProperty("--primary", client.cor);
  document.documentElement.style.setProperty("--primary-dark", client.corEscura);

  function clientAllows(id) {
    const item = config.modulos[id];
    if (!item) return false;
    const included = client.modulos.includes(id);
    const roleAllowed = item.perfis.includes(roleKey);
    return included && roleAllowed;
  }

  function typeLabel(item) {
    if (item.tipo === "comum") return "Recurso para todos";
    if (item.tipo === "exclusivo") return "Exclusivo do Instituto Atlas";
    if (item.tipo === "por perfil") return "Acesso específico por perfil";
    return "Módulo opcional por cliente";
  }

  function nav() {
    return Object.entries(config.modulos)
      .filter(([id]) => clientAllows(id))
      .map(([id, item]) => `<a class="${id === page ? "active" : ""}" href="${id}.html">${item.icone} ${item.titulo}</a>`)
      .join("");
  }

  function selectors() {
    const clients = Object.entries(config.clientes)
      .map(([key, value]) => `<option value="${key}" ${key === clientKey ? "selected" : ""}>${U.esc(value.nome)}</option>`)
      .join("");
    const roles = Object.entries(config.perfis)
      .map(([key, value]) => `<option value="${key}" ${key === roleKey ? "selected" : ""}>${value}</option>`)
      .join("");
    const users = usuarios.length
      ? usuarios.map((u) => `<option value="${u.id}" ${usuario && u.id === usuario.id ? "selected" : ""}>${U.esc(u.nome)}</option>`).join("")
      : '<option value="">Nenhum usuário</option>';
    return `<div class="selectors">
      <label>Cliente<select id="clientSelect">${clients}</select></label>
      <label>Perfil de acesso<select id="roleSelect">${roles}</select></label>
      <label>Usuário<select id="userSelect" ${usuarios.length ? "" : "disabled"}>${users}</select></label>
    </div>`;
  }

  function explanation() {
    if (module.tipo === "comum") {
      return `<strong>Alta variabilidade:</strong> esta é uma interface comum, reutilizada por todos os clientes. Os dados, as cores e os termos mudam pela configuração, mas a página-base é a mesma.`;
    }
    if (module.tipo === "exclusivo") {
      return `<strong>Função específica separada:</strong> o Laboratório Inteligente é um módulo exclusivo do Instituto Técnico Atlas. Ele possui arquivo e regra próprios e não aparece para os demais clientes.`;
    }
    if (module.tipo === "por perfil") {
      return `<strong>Alta variabilidade por usuário:</strong> esta interface só aparece para perfis autorizados. O cliente utiliza a mesma base, porém cada usuário enxerga apenas as funções necessárias.`;
    }
    return `<strong>Alta variabilidade por módulo:</strong> esta interface é opcional. Ela é ativada somente no pacote dos clientes que contrataram ou precisam deste recurso.`;
  }

  function shell(content) {
    const quem = usuario ? ` • ${U.esc(usuario.nome)}` : "";
    return `<div class="app">
      <aside class="sidebar">
        <a class="brand" href="dashboard.html"><span class="brand-icon">📖</span><div><h1>StudyFlow</h1><small>Protótipo configurável</small></div></a>
        <div class="client-name">${U.esc(client.nome)}</div>
        <nav>${nav()}</nav>
        <div class="legend"><strong>Tipos de variação</strong>Comum • Opcional<br>Por perfil • Exclusivo</div>
      </aside>
      <main class="content">
        <header class="topbar"><div><h2>${module.titulo}</h2><p>${U.esc(client.nome)} • ${config.perfis[roleKey]}${quem}</p></div>${selectors()}</header>
        <div class="variation-note">${explanation()}</div>
        ${content}
      </main>
    </div>`;
  }

  const carregando = (texto) => `<div id="crud-root"><div class="card"><p>${texto}</p></div></div>`;

  function dashboard() {
    const moduleCards = Object.entries(config.modulos).map(([id, item]) => {
      const active = clientAllows(id);
      const reason = item.clienteExclusivo && clientKey !== item.clienteExclusivo
        ? "Outro cliente"
        : !item.perfis.includes(roleKey) ? "Outro perfil" : active ? "Ativo" : "Não contratado";
      return `<div class="card"><div class="row"><strong>${item.icone} ${item.titulo}</strong><span class="tag ${item.tipo}">${item.tipo}</span></div><p>${typeLabel(item)}</p><span class="tag ${active ? "ativo" : ""}">${reason}</span></div>`;
    }).join("");
    return `<div class="grid four">
      <div class="card"><span class="small">Módulos disponíveis</span><div class="number">${client.modulos.length}</div></div>
      <div class="card"><span class="small">Perfil atual</span><div class="number" style="font-size:20px">${config.perfis[roleKey]}</div>${usuario ? `<div class="small">${U.esc(usuario.nome)}</div>` : ""}</div>
      <div class="card"><span class="small">Método de avaliação</span><div class="number" style="font-size:18px">${U.esc(client.media)}</div></div>
      <div class="card"><span class="small">Lembretes</span><div class="number" style="font-size:18px">${U.esc(client.lembretes)}</div></div>
    </div>
    <div style="margin-top:16px" id="crud-root"><div class="card"><p>Carregando resumo...</p></div></div>
    <div class="card" style="margin-top:16px"><h3>Mapa das 12 interfaces</h3><p>Troque o cliente e o perfil no topo. O menu e os cartões abaixo demonstram quais partes são comuns, opcionais, restritas ou exclusivas.</p></div>
    <div class="grid three" style="margin-top:16px">${moduleCards}</div>`;
  }

  function arquitetura() {
    return `<div class="card"><h3>Arquitetura escolhida: Linha de Produto de Software modular</h3><p>A arquitetura usa uma base comum e pontos de variação controlados por configuração. Cada versão é montada com componentes reutilizáveis, módulos opcionais, permissões por perfil e temas por cliente.</p></div>
      <div class="architecture-flow" style="margin-top:16px"><div class="card"><h3>1. Configuração</h3><p>Cliente, tema, termos, módulos e regras.</p></div><div class="card"><h3>2. Núcleo comum</h3><p>Login, disciplinas, tarefas e calendário.</p></div><div class="card"><h3>3. Plugins</h3><p>Provas, avisos, sessões e laboratório.</p></div><div class="card"><h3>4. Produto</h3><p>Versão específica entregue ao cliente.</p></div></div>
      <div class="grid two" style="margin-top:16px"><div class="card"><h3>Tecnologias sugeridas</h3><div class="table-wrap"><table><tbody>
        <tr><th>Linguagem</th><td>TypeScript no front-end e no back-end</td></tr><tr><th>Interface</th><td>React com biblioteca própria de componentes</td></tr><tr><th>Back-end</th><td>Node.js em módulos ou serviços</td></tr><tr><th>SGBD principal</th><td>PostgreSQL com separação por instituição</td></tr><tr><th>Acesso</th><td>RBAC: estudante, professor e administrador</td></tr><tr><th>Variação</th><td>Feature flags, configuração por cliente e Strategy para médias</td></tr><tr><th>Testes</th><td>Testes comuns + testes de cada módulo ativado</td></tr>
      </tbody></table></div></div><div class="card"><h3>Exemplo do ponto de variação</h3><pre>cliente: "atlas"
modulos: ["tarefas", "provas", "laboratorio"]
tema: "azul"
calculoDeMedia: "competencias"
perfisDoLaboratorio: ["professor", "administrador"]</pre><p>Uma nova versão troca a configuração e reutiliza os mesmos componentes.</p></div></div>
      <div class="card sources" style="margin-top:16px"><h3>Fontes pesquisadas</h3><ul><li><a href="https://www.sei.cmu.edu/library/software-product-lines-collection/" target="_blank" rel="noreferrer">SEI/Carnegie Mellon — Software Product Lines Collection</a></li><li><a href="https://react.dev/learn/your-first-component" target="_blank" rel="noreferrer">React — componentes reutilizáveis</a></li><li><a href="https://www.typescriptlang.org/" target="_blank" rel="noreferrer">TypeScript — JavaScript com tipos</a></li><li><a href="https://www.postgresql.org/docs/current/ddl-rowsecurity.html" target="_blank" rel="noreferrer">PostgreSQL — políticas de segurança por linha</a></li></ul></div>`;
  }

  const renderers = {
    dashboard,
    disciplinas: () => carregando("Carregando..."),
    tarefas: () => carregando("Carregando tarefas..."),
    calendario: () => carregando("Carregando calendário..."),
    sessoes: () => carregando("Carregando sessões..."),
    desempenho: () => carregando("Carregando desempenho..."),
    avisos: () => carregando("Carregando avisos..."),
    provas: () => carregando("Carregando..."),
    usuarios: () => carregando("Carregando usuários..."),
    personalizacao: () => carregando("Carregando configuração..."),
    laboratorio: () => carregando("Carregando laboratório..."),
    arquitetura
  };

  function denied() {
    const exclusiveReason = module.clienteExclusivo && clientKey !== module.clienteExclusivo;
    return `<div class="denied card"><div class="symbol">🔒</div><h3>Interface não disponível nesta versão</h3><p>${exclusiveReason ? "Esta função foi criada exclusivamente para o Instituto Técnico Atlas." : "O cliente ou o perfil selecionado não possui acesso a este módulo."}</p><p>Troque o cliente ou o perfil no topo para demonstrar a variabilidade do sistema.</p><a class="button" href="dashboard.html">Voltar ao painel</a></div>`;
  }

  const permitido = clientAllows(page);
  app.innerHTML = shell(permitido ? renderers[page]() : denied());

  // Telas ligadas ao banco: o crud.js e o paginas.js montam o conteúdo dentro de #crud-root
  const montar = window.StudyFlowPaginas && window.StudyFlowPaginas[page];
  if (permitido && montar) {
    montar({
      root: document.getElementById("crud-root"), clientKey, roleKey, client, usuario, usuarios, config,
      typeLabel
    });
  }

  document.getElementById("clientSelect").addEventListener("change", function () {
    localStorage.setItem("studyflow_cliente", this.value);
    location.reload();
  });
  document.getElementById("roleSelect").addEventListener("change", function () {
    localStorage.setItem("studyflow_perfil", this.value);
    location.reload();
  });
  document.getElementById("userSelect").addEventListener("change", function () {
    localStorage.setItem(chaveUsuario, this.value);
    location.reload();
  });
})();
