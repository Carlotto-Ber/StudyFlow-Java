/*
  Telas ligadas ao banco de dados (além de Tarefas, Avisos e Provas, que ficam no crud.js):
  Painel, Disciplinas, Calendário, Sessões de estudo, Desempenho, Usuários, Personalização e Laboratório.
  Cada tela recebe o contexto montado pelo app.js (instituição, perfil e usuário atuais).
*/
(function () {
  const api = window.StudyFlowAPI;
  const U = window.StudyFlowUtil;
  const { esc, dataBr, dataHoraBr, hora, toast, plural, registrarPagina, estudanteAtual } = U;

  const TIPOS_EVENTO = { tarefa: "Entrega", prova: "Prova", sessao: "Estudo" };

  function mostrarErro(root, mensagem) {
    const alvo = root.querySelector("[data-erro]");
    if (alvo) { alvo.textContent = mensagem; alvo.hidden = false; }
  }

  // ====================================================================== PAINEL
  registrarPagina("dashboard", async (ctx) => {
    const estudante = estudanteAtual(ctx);
    const t = ctx.client.termos;
    const p = await api.get(`/painel?instituicao=${ctx.client.id}${estudante ? "&estudante=" + estudante : ""}`);
    const numeros = [
      [plural(t.disciplina), p.disciplinas], ["Tarefas", p.tarefas], [plural(t.prova), p.provas], ["Avisos", p.avisos],
      ...(estudante ? [] : [[plural(t.aluno), p.estudantes], ["Professores", p.professores]])
    ].filter(([, valor]) => valor !== null && valor !== undefined);

    const proximos = p.proximos.map((e) => `<div class="row">
        <div><strong>${esc(e.titulo)}</strong>${e.disciplina ? `<div class="small">${esc(e.disciplina)}</div>` : ""}</div>
        <span class="tag ${e.tipo === "prova" ? "opcional" : "ativo"}">${dataBr(e.data)}</span></div>`).join("");

    ctx.root.innerHTML = `<div class="card"><h3>Resumo ${estudante ? "de " + esc(ctx.usuario.nome) : "de " + esc(ctx.client.nome)}</h3>
        <div class="grid four" style="margin-top:12px">${numeros.map(([rotulo, valor]) =>
          `<div><span class="small">${esc(rotulo)}</span><div class="number">${valor}</div></div>`).join("")}</div></div>
      <div class="card" style="margin-top:16px"><h3>Próximos prazos</h3>
        ${proximos || '<p class="small">Nenhum prazo nos próximos meses.</p>'}</div>`;
  });

  // ====================================================================== DISCIPLINAS
  registrarPagina("disciplinas", async (ctx) => {
    const root = ctx.root;
    const idInst = ctx.client.id;
    const termo = ctx.client.termos.disciplina;
    const estudante = estudanteAtual(ctx);
    const podeEditar = ctx.roleKey !== "estudante" || ctx.clientKey === "individual";
    const gerenciaMatriculas = ctx.roleKey !== "estudante";
    const estado = { itens: [], professores: [], formAberto: false, editando: null, matriculas: null };

    async function carregar() {
      estado.itens = await api.get(`/disciplinas?instituicao=${idInst}${estudante ? "&estudante=" + estudante : ""}`);
    }

    function formulario() {
      if (!estado.formAberto) return "";
      const e = estado.editando;
      const idProfessor = e ? e.id_professor : (ctx.roleKey === "professor" && ctx.usuario ? ctx.usuario.id : "");
      const professores = estado.professores.length ? `<label class="field">Professor responsável
          <select id="d-professor"><option value="">Sem professor</option>${estado.professores.map((p) =>
            `<option value="${p.id}" ${p.id === idProfessor ? "selected" : ""}>${esc(p.nome)}</option>`).join("")}</select></label>` : "";
      return `<form class="crud-form" data-form>
        <h3>${e ? "Editar" : "Cadastro de"} ${esc(termo.toLowerCase())}</h3>
        <div class="form-grid">
          <label class="field wide">Nome<input id="d-nome" maxlength="150" required value="${e ? esc(e.nome) : ""}"></label>
          <label class="field">Créditos<input id="d-creditos" type="number" min="1" max="30" required value="${e ? e.creditos : 4}"></label>
          ${professores}
          <label class="field wide">Descrição (opcional)<textarea id="d-descricao" rows="2">${e ? esc(e.descricao || "") : ""}</textarea></label>
        </div>
        <div class="alert" data-erro hidden></div>
        <div class="form-actions"><button class="button" type="submit">Salvar</button>
          <button class="button secondary" type="button" data-acao="cancelar">Cancelar</button></div></form>`;
    }

    function tabela() {
      const colunas = [termo, "Responsável", ctx.client.termos.aluno + "s", "Tarefas", "Créditos"];
      const linhas = estado.itens.map((d) => `<tr>
        <td><strong>${esc(d.nome)}</strong>${d.descricao ? `<div class="small">${esc(d.descricao)}</div>` : ""}</td>
        <td>${d.professor_nome ? esc(d.professor_nome) : '<span class="small">—</span>'}</td>
        <td>${d.total_estudantes}</td><td>${d.total_tarefas}</td><td>${d.creditos}</td>
        ${podeEditar ? `<td><div class="form-actions">
          ${gerenciaMatriculas ? `<button class="button secondary" data-acao="matriculas" data-id="${d.id}">Matrículas</button>` : ""}
          <button class="button secondary" data-acao="editar" data-id="${d.id}">Editar</button>
          <button class="button danger" data-acao="excluir" data-id="${d.id}">Excluir</button></div></td>` : ""}</tr>`).join("");
      const vazio = `<tr><td colspan="${colunas.length + (podeEditar ? 1 : 0)}" class="small">Nenhum registro cadastrado.</td></tr>`;
      return `<div class="table-wrap"><table><thead><tr>${colunas.map((c) => `<th>${esc(c)}</th>`).join("")}${podeEditar ? "<th>Ações</th>" : ""}</tr></thead>
        <tbody>${linhas || vazio}</tbody></table></div>`;
    }

    function painelMatriculas() {
      if (!estado.matriculas) return "";
      const { disciplina, alunos } = estado.matriculas;
      return `<div class="card" style="margin-top:16px"><div class="row"><h3>Matrículas — ${esc(disciplina.nome)}</h3>
        <button class="button secondary" data-acao="fechar-matriculas">Fechar</button></div>
        <p class="small">Marque quem cursa esta ${esc(termo.toLowerCase())}. Só os matriculados recebem notificações e eventos de calendário.</p>
        ${alunos.map((a) => `<div class="switch-row"><label><input type="checkbox" data-matricula="${a.id}" ${a.matriculado ? "checked" : ""}>
          <strong>${esc(a.nome)}</strong> <span class="small">${esc(a.matricula)}</span></label></div>`).join("") || '<p class="small">Nenhum estudante cadastrado.</p>'}</div>`;
    }

    function render() {
      root.innerHTML = `<div class="card"><div class="row"><div><h3>${esc(plural(termo))} — ${esc(ctx.client.nome)}</h3>
          <span class="small">${estudante ? "Somente as matérias em que você está matriculado." : "A mesma tabela atende clientes com termos diferentes."}</span></div>
          ${podeEditar ? `<button class="button" data-acao="novo">+ Nova ${esc(termo.toLowerCase())}</button>` : ""}</div>
          ${formulario()}${tabela()}</div>${painelMatriculas()}`;
    }

    async function salvar() {
      const v = (s) => root.querySelector(s).value;
      const professor = root.querySelector("#d-professor");
      const dados = { nome: v("#d-nome"), descricao: v("#d-descricao"), creditos: Number(v("#d-creditos")) };
      if (professor) dados.id_professor = professor.value ? Number(professor.value) : null;
      if (!estado.editando) {
        dados.id_instituicao = idInst;
        if (estudante) dados.matricular_estudante = estudante; // quem cria já fica matriculado
      }
      const botao = root.querySelector("[data-form] button[type=submit]");
      botao.disabled = true;
      try {
        if (estado.editando) await api.put(`/disciplinas/${estado.editando.id}`, dados);
        else await api.post("/disciplinas", dados);
        toast("Alterações salvas");
        Object.assign(estado, { formAberto: false, editando: null });
        await carregar();
        render();
      } catch (erro) {
        mostrarErro(root, erro.message);
        botao.disabled = false;
      }
    }

    root.addEventListener("submit", (ev) => { ev.preventDefault(); salvar(); });

    root.addEventListener("change", async (ev) => {
      const caixa = ev.target.closest("[data-matricula]");
      if (!caixa) return;
      try {
        const alunos = await api.put(`/disciplinas/${estado.matriculas.disciplina.id}/matriculas`,
          { id_estudante: Number(caixa.dataset.matricula), matriculado: caixa.checked });
        estado.matriculas.alunos = alunos;
        await carregar();
        render();
      } catch (erro) {
        toast(erro.message);
        caixa.checked = !caixa.checked;
      }
    });

    root.addEventListener("click", async (ev) => {
      const el = ev.target.closest("[data-acao]");
      if (!el) return;
      const id = Number(el.dataset.id);
      try {
        switch (el.dataset.acao) {
          case "novo": Object.assign(estado, { formAberto: true, editando: null }); render(); root.querySelector("#d-nome")?.focus(); break;
          case "editar": Object.assign(estado, { formAberto: true, editando: estado.itens.find((d) => d.id === id) }); render(); break;
          case "cancelar": Object.assign(estado, { formAberto: false, editando: null }); render(); break;
          case "excluir":
            if (!confirm("Excluir esta disciplina? Tarefas, provas e avisos ligados a ela também serão excluídos.")) return;
            await api.del(`/disciplinas/${id}`);
            toast("Registro excluído");
            if (estado.matriculas && estado.matriculas.disciplina.id === id) estado.matriculas = null;
            await carregar();
            render();
            break;
          case "matriculas":
            estado.matriculas = { disciplina: estado.itens.find((d) => d.id === id), alunos: await api.get(`/disciplinas/${id}/matriculas`) };
            render();
            break;
          case "fechar-matriculas": estado.matriculas = null; render(); break;
          default:
        }
      } catch (erro) {
        toast(erro.message);
      }
    });

    if (ctx.roleKey !== "estudante") estado.professores = await api.get(`/usuarios?instituicao=${idInst}&tipo=professor`);
    await carregar();
    render();
  });

  // ====================================================================== CALENDÁRIO
  registrarPagina("calendario", async (ctx) => {
    const root = ctx.root;
    const estudante = estudanteAtual(ctx);
    const NOMES_MES = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];
    const hoje = new Date();
    const estado = { ano: hoje.getFullYear(), mes: hoje.getMonth() + 1, eventos: [] };

    let ultimaRequisicao = 0; // cliques rápidos: só vale a resposta mais recente
    async function carregar() {
      const numero = ++ultimaRequisicao;
      const mes = `${estado.ano}-${String(estado.mes).padStart(2, "0")}`;
      const eventos = await api.get(`/calendario?instituicao=${ctx.client.id}&mes=${mes}${estudante ? "&estudante=" + estudante : ""}`);
      if (numero === ultimaRequisicao) estado.eventos = eventos;
    }

    function render() {
      const diasNoMes = new Date(estado.ano, estado.mes, 0).getDate();
      const deslocamento = (new Date(estado.ano, estado.mes - 1, 1).getDay() + 6) % 7; // semana começa na segunda
      const prefixo = `${estado.ano}-${String(estado.mes).padStart(2, "0")}-`;
      const ehHoje = (d) => hoje.getFullYear() === estado.ano && hoje.getMonth() + 1 === estado.mes && hoje.getDate() === d;
      const celulas = [];
      for (let i = 0; i < deslocamento; i++) celulas.push('<div class="day vazio"></div>');
      for (let d = 1; d <= diasNoMes; d++) {
        const doDia = estado.eventos.filter((e) => e.data === prefixo + String(d).padStart(2, "0"));
        celulas.push(`<div class="day ${ehHoje(d) ? "hoje" : ""}"><strong>${d}</strong>${doDia.map((e) =>
          `<div class="event ${esc(e.tipo)}" title="${esc(e.titulo)}">${esc(e.titulo)}</div>`).join("")}</div>`);
      }
      const cabecalho = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"].map((d) => `<div class="cal-head">${d}</div>`).join("");
      const lista = estado.eventos.map((e) => `<div class="row"><div><strong>${esc(e.titulo)}</strong>${e.disciplina ? `<div class="small">${esc(e.disciplina)}</div>` : ""}</div>
        <div><span class="tag ${e.tipo === "prova" ? "opcional" : e.tipo === "sessao" ? "comum" : "ativo"}">${dataBr(e.data)}</span></div></div>`).join("");
      root.innerHTML = `<div class="card"><div class="row"><div class="form-actions" style="margin:0">
          <button class="button secondary" data-acao="anterior" aria-label="Mês anterior">‹</button>
          <h3 style="margin:0 8px">${NOMES_MES[estado.mes - 1]} de ${estado.ano}</h3>
          <button class="button secondary" data-acao="proximo" aria-label="Próximo mês">›</button>
          <button class="button secondary" data-acao="hoje">Hoje</button></div>
          <span class="tag ativo">Calendário ${estudante ? "pessoal" : "institucional"}</span></div>
        <div class="calendar">${cabecalho}${celulas.join("")}</div>
        <p class="small">Legenda: <span class="tag ativo">entrega</span> <span class="tag opcional">prova</span> <span class="tag comum">estudo</span>
          ${estudante ? "— eventos criados para você pelos observers." : "— entregas e provas de toda a instituição."}</p></div>
        <div class="card" style="margin-top:16px"><h3>Eventos do mês</h3>${lista || '<p class="small">Nenhum evento neste mês.</p>'}</div>`;
    }

    root.addEventListener("click", async (ev) => {
      const el = ev.target.closest("[data-acao]");
      if (!el) return;
      const acao = el.dataset.acao;
      if (acao === "anterior" || acao === "proximo") {
        const data = new Date(estado.ano, estado.mes - 1 + (acao === "proximo" ? 1 : -1), 1);
        estado.ano = data.getFullYear();
        estado.mes = data.getMonth() + 1;
      } else if (acao === "hoje") {
        estado.ano = hoje.getFullYear();
        estado.mes = hoje.getMonth() + 1;
      } else {
        return;
      }
      try {
        await carregar();
        render();
      } catch (erro) {
        toast(erro.message);
      }
    });

    await carregar();
    render();
  });

  // ====================================================================== SESSÕES DE ESTUDO
  registrarPagina("sessoes", async (ctx) => {
    const root = ctx.root;
    const estudante = estudanteAtual(ctx);
    const idInst = ctx.client.id;
    const estado = { disciplinas: [], sessoes: [], resumo: { minutos: 0, sessoes: 0, maior_foco: null }, timer: null, intervalo: null };

    async function carregar() {
      const dados = await api.get(`/sessoes?instituicao=${idInst}${estudante ? "&estudante=" + estudante : ""}`);
      estado.sessoes = dados.sessoes;
      estado.resumo = dados.resumo;
    }


    function relogio() {
      if (!estado.timer) return "00:25:00";
      const restante = Math.max(0, estado.timer.duracao * 60 - Math.floor((Date.now() - estado.timer.inicio.getTime()) / 1000));
      const h = Math.floor(restante / 3600), m = Math.floor((restante % 3600) / 60), s = restante % 60;
      return [h, m, s].map((n) => String(n).padStart(2, "0")).join(":");
    }

    function cartaoCronometro() {
      if (!estudante) {
        return `<div class="card"><h3>Acompanhamento</h3><p>Você está vendo as sessões de estudo de toda a instituição. O cronômetro aparece para o perfil Estudante.</p></div>`;
      }
      const t = estado.timer;
      const disciplinas = estado.disciplinas.map((d) => `<option value="${d.id}" ${t && t.idDisciplina === d.id ? "selected" : ""}>${esc(d.nome)}</option>`).join("");
      return `<div class="card"><h3>Iniciar sessão de estudo</h3>
        <div class="number" id="timer-display">${t ? relogio() : "00:25:00"}</div>
        <p>Técnica Pomodoro: sessões de foco com tempo definido.</p>
        <div class="form-grid">
          <label class="field">${esc(ctx.client.termos.disciplina)}<select id="s-disciplina" ${t ? "disabled" : ""}><option value="">Sem ${esc(ctx.client.termos.disciplina.toLowerCase())}</option>${disciplinas}</select></label>
          <label class="field">Duração (min)<input id="s-duracao" type="number" min="1" max="180" value="${t ? t.duracao : 25}" ${t ? "disabled" : ""}></label>
          <label class="field wide">O que você vai estudar?<input id="s-titulo" maxlength="150" value="${t ? esc(t.titulo) : ""}" placeholder="Ex.: Revisão de listas" ${t ? "disabled" : ""}></label>
        </div>
        <div class="form-actions">${t
          ? '<button class="button" data-acao="concluir">Concluir e salvar</button><button class="button secondary" data-acao="descartar">Descartar</button>'
          : '<button class="button" data-acao="iniciar">Iniciar</button>'}</div></div>`;
    }

    function cartaoResumo() {
      const r = estado.resumo;
      return `<div class="card"><h3>Resumo semanal</h3>
        <div class="row"><span>Tempo estudado</span><strong>${U.minutosParaTexto(r.minutos)}</strong></div>
        <div class="row"><span>Sessões registradas</span><strong>${r.sessoes}</strong></div>
        <div class="row"><span>Maior foco</span><strong>${r.maior_foco ? esc(r.maior_foco) : "—"}</strong></div>
        <p class="small">Considera os últimos 7 dias${estudante ? "" : " de toda a instituição"}.</p></div>`;
    }

    function tabela() {
      const linhas = estado.sessoes.map((s) => `<tr>
        <td>${dataHoraBr(s.data_inicio)}</td><td><strong>${esc(s.titulo)}</strong></td>
        <td>${s.disciplina ? esc(s.disciplina) : '<span class="small">—</span>'}</td>
        <td>${U.minutosParaTexto(s.minutos)}</td>
        ${estudante ? `<td><button class="button danger" data-acao="excluir" data-id="${s.id}">Excluir</button></td>` : `<td>${esc(s.estudante)}</td>`}</tr>`).join("");
      return `<div class="card" style="margin-top:16px"><h3>Sessões recentes</h3><div class="table-wrap"><table>
        <thead><tr><th>Início</th><th>Conteúdo</th><th>${esc(ctx.client.termos.disciplina)}</th><th>Duração</th><th>${estudante ? "Ações" : esc(ctx.client.termos.aluno)}</th></tr></thead>
        <tbody>${linhas || '<tr><td colspan="5" class="small">Nenhuma sessão registrada.</td></tr>'}</tbody></table></div></div>`;
    }

    function render() {
      root.innerHTML = `<div class="grid two">${cartaoCronometro()}${cartaoResumo()}</div>${tabela()}`;
    }

    function parar() {
      clearInterval(estado.intervalo);
      estado.intervalo = null;
    }

    function tique() {
      if (!estado.timer) return;
      const alvo = root.querySelector("#timer-display");
      if (alvo) alvo.textContent = relogio();
      if (Date.now() - estado.timer.inicio.getTime() >= estado.timer.duracao * 60000) concluir(true);
    }

    function iniciar() {
      const titulo = root.querySelector("#s-titulo").value.trim();
      if (!titulo) { toast("Informe o que você vai estudar."); root.querySelector("#s-titulo").focus(); return; }
      const duracao = Number(root.querySelector("#s-duracao").value) || 25;
      const disciplina = root.querySelector("#s-disciplina").value;
      estado.timer = { inicio: new Date(), duracao, titulo, idDisciplina: disciplina ? Number(disciplina) : null };
      estado.intervalo = setInterval(tique, 1000);
      render();
    }

    async function concluir(automatico) {
      const t = estado.timer;
      if (!t) return;
      parar();
      const fim = automatico ? new Date(t.inicio.getTime() + t.duracao * 60000) : new Date();
      estado.timer = null;
      try {
        await api.post("/sessoes", { id_estudante: estudante, id_disciplina: t.idDisciplina, titulo: t.titulo,
          data_inicio: U.dataHoraLocal(t.inicio), data_fim: U.dataHoraLocal(fim) });
        toast(automatico ? "Tempo esgotado: sessão salva" : "Sessão salva");
        await carregar();
      } catch (erro) {
        toast(erro.message);
      }
      render();
    }

    root.addEventListener("click", async (ev) => {
      const el = ev.target.closest("[data-acao]");
      if (!el) return;
      try {
        if (el.dataset.acao === "iniciar") iniciar();
        else if (el.dataset.acao === "concluir") await concluir(false);
        else if (el.dataset.acao === "descartar") { parar(); estado.timer = null; render(); }
        else if (el.dataset.acao === "excluir") {
          if (!confirm("Excluir esta sessão?")) return;
          await api.del(`/sessoes/${el.dataset.id}`);
          toast("Sessão excluída");
          await carregar();
          render();
        }
      } catch (erro) {
        toast(erro.message);
      }
    });

    if (estudante) estado.disciplinas = await api.get(`/disciplinas?instituicao=${idInst}&estudante=${estudante}`);
    await carregar();
    render();
  });

  // ====================================================================== DESEMPENHO
  registrarPagina("desempenho", async (ctx) => {
    const estudante = estudanteAtual(ctx);
    const d = await api.get(`/desempenho?instituicao=${ctx.client.id}${estudante ? "&estudante=" + estudante : ""}`);
    const linhas = d.disciplinas.map((x) => `<div class="row">
        <span>${esc(x.nome)}<div class="small">${x.creditos} créditos · ${x.avaliacoes} avaliações</div></span>
        <div style="width:45%"><div class="progress"><span style="width:${x.percentual}%"></span></div></div>
        <strong>${esc(x.rotulo)}</strong></div>`).join("");
    ctx.root.innerHTML = `<div class="grid two"><div class="card">
        <h3>Desempenho por ${esc(ctx.client.termos.disciplina.toLowerCase())}</h3>
        <p class="small">${estudante ? "Seu desempenho nas avaliações já corrigidas." : "Média da turma em cada " + esc(ctx.client.termos.disciplina.toLowerCase()) + "."}</p>
        ${linhas || '<p class="small">Nenhuma disciplina encontrada.</p>'}
        <div class="row"><strong>Resultado geral</strong><strong class="number" style="font-size:22px">${esc(d.geral.rotulo)}</strong></div></div>
      <div class="card"><h3>Regra variável de cálculo</h3><p>Cliente atual:</p>
        <div class="number" style="font-size:21px">${esc(d.descricao)}</div>
        <p>O componente visual é reutilizado, enquanto a estratégia de cálculo (padrão Strategy) muda conforme a configuração da instituição no banco de dados.</p></div></div>`;
  });

  // ====================================================================== USUÁRIOS
  registrarPagina("usuarios", async (ctx) => {
    const root = ctx.root;
    const idInst = ctx.client.id;
    const estado = { lista: [], formAberto: false };
    const rotuloPerfil = { administrador: "Administrador", professor: "Professor", estudante: ctx.client.termos.aluno };

    async function carregar() {
      estado.lista = await api.get(`/usuarios?instituicao=${idInst}`);
    }

    function permissoes(tipo) {
      const titulos = Object.entries(ctx.config.modulos)
        .filter(([id, m]) => m.perfis.includes(tipo) && ctx.client.modulos.includes(id))
        .map(([, m]) => m.titulo);
      const visiveis = titulos.slice(0, 4).map((t) => `<span class="tag">${esc(t)}</span>`).join("");
      return `<div class="permissions">${visiveis}${titulos.length > 4 ? `<span class="tag">+${titulos.length - 4}</span>` : ""}</div>`;
    }

    function formulario() {
      if (!estado.formAberto) return "";
      return `<form class="crud-form" data-form><h3>Novo usuário</h3><div class="form-grid">
        <label class="field">Perfil<select id="u-tipo">${Object.entries(rotuloPerfil).map(([k, v]) => `<option value="${k}">${esc(v)}</option>`).join("")}</select></label>
        <label class="field" id="u-matricula-campo" hidden>Matrícula<input id="u-matricula" maxlength="50"></label>
        <label class="field wide">Nome<input id="u-nome" maxlength="150" required></label>
        <label class="field">E-mail<input id="u-email" type="email" maxlength="150" required></label>
        <label class="field">Telefone (com DDD)<input id="u-telefone" required placeholder="41 99999-0000"></label>
        <label class="field">CPF<input id="u-cpf" required placeholder="000.000.000-00"></label>
        <label class="field">Senha<input id="u-senha" type="password" minlength="6" required></label></div>
        <div class="alert" data-erro hidden></div>
        <div class="form-actions"><button class="button" type="submit">Cadastrar</button>
          <button class="button secondary" type="button" data-acao="cancelar">Cancelar</button></div></form>`;
    }

    function render() {
      const linhas = estado.lista.map((u) => `<tr><td><strong>${esc(u.nome)}</strong>${u.matricula ? `<div class="small">${esc(u.matricula)}</div>` : ""}</td>
        <td>${esc(rotuloPerfil[u.tipo])}</td><td>${esc(u.email)}<div class="small">${esc(u.telefone)}</div></td>
        <td>${permissoes(u.tipo)}</td>
        <td><button class="button danger" data-acao="remover" data-tipo="${u.tipo}" data-id="${u.id}">Remover</button></td></tr>`).join("");
      root.innerHTML = `<div class="card"><div class="row"><h3>Gerenciamento de usuários</h3>
          <button class="button" data-acao="novo">+ Novo usuário</button></div>${formulario()}
        <div class="table-wrap"><table><thead><tr><th>Nome</th><th>Perfil</th><th>Contato</th><th>Permissões</th><th>Ações</th></tr></thead>
          <tbody>${linhas || '<tr><td colspan="5" class="small">Nenhum usuário.</td></tr>'}</tbody></table></div></div>`;
    }

    async function salvar() {
      const v = (s) => root.querySelector(s).value;
      const tipo = v("#u-tipo");
      const dados = { tipo, id_instituicao: idInst, nome: v("#u-nome"), email: v("#u-email"), telefone: v("#u-telefone"), cpf: v("#u-cpf"), senha: v("#u-senha") };
      if (tipo === "estudante") dados.matricula = v("#u-matricula");
      const botao = root.querySelector("[data-form] button[type=submit]");
      botao.disabled = true;
      try {
        await api.post("/cadastro", dados);
        toast("Usuário cadastrado");
        estado.formAberto = false;
        await carregar();
        render();
      } catch (erro) {
        mostrarErro(root, erro.message);
        botao.disabled = false;
      }
    }

    root.addEventListener("submit", (ev) => { ev.preventDefault(); salvar(); });
    root.addEventListener("change", (ev) => {
      if (ev.target.id === "u-tipo") root.querySelector("#u-matricula-campo").hidden = ev.target.value !== "estudante";
    });
    root.addEventListener("click", async (ev) => {
      const el = ev.target.closest("[data-acao]");
      if (!el) return;
      try {
        if (el.dataset.acao === "novo") { estado.formAberto = true; render(); root.querySelector("#u-nome")?.focus(); }
        else if (el.dataset.acao === "cancelar") { estado.formAberto = false; render(); }
        else if (el.dataset.acao === "remover") {
          if (!confirm("Remover este usuário?")) return;
          await api.del(`/usuarios/${el.dataset.tipo}/${el.dataset.id}`);
          toast("Usuário removido");
          await carregar();
          render();
        }
      } catch (erro) {
        toast(erro.message);
      }
    });

    await carregar();
    render();
  });

  // ====================================================================== PERSONALIZAÇÃO
  registrarPagina("personalizacao", async (ctx) => {
    const root = ctx.root;
    const c = ctx.client;
    const METODOS = { nenhuma: "Sem cálculo institucional", aritmetica: "Média aritmética", ponderada: "Média ponderada por créditos", conceito: "Competências e conceito" };
    const CORES = ["#4f46c8", "#1d7a66", "#a14b18", "#1769aa"];

    const interruptor = (atributo, valor, ligado, editavel) =>
      `<span class="switch ${ligado ? "on" : ""} ${editavel ? "" : "travado"}" role="switch" aria-checked="${ligado}" ${atributo}="${valor}"></span>`;

    const modulos = Object.entries(ctx.config.modulos).map(([id, m]) => {
      const editavel = c.editaveis.includes(id);
      return `<div class="switch-row"><div><strong>${esc(m.titulo)}</strong><div class="small">${esc(ctx.typeLabel(m))}${editavel ? "" : " · fixo"}</div></div>
        ${interruptor("data-modulo", id, c.modulos.includes(id), editavel)}</div>`;
    }).join("");

    const canais = [["sistema", "Sistema (sempre ativo)"], ["email", "E-mail"], ["app", "Aplicativo"], ["sms", "SMS"]].map(([id, nome]) =>
      `<div class="switch-row"><div><strong>${nome}</strong></div>${interruptor("data-canal", id, c.canais.includes(id), id !== "sistema")}</div>`).join("");

    root.innerHTML = `<div class="grid two"><div class="card"><h3>Identidade do cliente</h3>
        <label class="field">Nome exibido<input id="p-nome" maxlength="150" value="${esc(c.nome)}"></label>
        <p><strong>Cor principal</strong></p>
        <div class="colors">${CORES.map((cor) => `<button type="button" class="color" style="background:${cor}" data-color="${cor}" aria-label="Cor ${cor}"></button>`).join("")}
          <input type="color" id="p-cor" value="${esc(c.cor)}" aria-label="Outra cor"></div>
        <p><strong>Vocabulário</strong></p><div class="form-grid">
          <label class="field">Disciplina<input id="p-termo-disciplina" maxlength="40" value="${esc(c.termos.disciplina)}"></label>
          <label class="field">Prova<input id="p-termo-prova" maxlength="40" value="${esc(c.termos.prova)}"></label>
          <label class="field">Aluno<input id="p-termo-aluno" maxlength="40" value="${esc(c.termos.aluno)}"></label>
          <label class="field">Método de avaliação<select id="p-metodo">${Object.entries(METODOS).map(([k, v]) =>
            `<option value="${k}" ${k === c.metodoMedia ? "selected" : ""}>${v}</option>`).join("")}</select></label></div>
        <p><strong>Canais de lembrete</strong></p>${canais}
        <div class="alert" data-erro hidden></div>
        <div class="form-actions"><button class="button" data-acao="salvar">Salvar configuração</button></div></div>
      <div class="card"><h3>Módulos desta versão</h3>
        <p class="small">Os módulos opcionais podem ser ligados e desligados. Os demais são fixos para esta instituição.</p>${modulos}</div></div>`;

    root.addEventListener("click", async (ev) => {
      const interruptorEl = ev.target.closest(".switch");
      if (interruptorEl) {
        if (!interruptorEl.classList.contains("travado")) {
          interruptorEl.classList.toggle("on");
          interruptorEl.setAttribute("aria-checked", interruptorEl.classList.contains("on"));
        }
        return;
      }
      const cor = ev.target.closest("[data-color]");
      if (cor) {
        root.querySelector("#p-cor").value = cor.dataset.color;
        document.documentElement.style.setProperty("--primary", cor.dataset.color);
        return;
      }
      if (!ev.target.closest("[data-acao=salvar]")) return;
      const v = (s) => root.querySelector(s).value;
      const ativos = (atributo) => [...root.querySelectorAll(`[${atributo}].on`)].map((el) => el.getAttribute(atributo));
      const dados = {
        nome: v("#p-nome"),
        cor: v("#p-cor"),
        termos: { disciplina: v("#p-termo-disciplina"), prova: v("#p-termo-prova"), aluno: v("#p-termo-aluno") },
        metodo_media: v("#p-metodo"),
        modulos: ativos("data-modulo").filter((m) => c.editaveis.includes(m)),
        canais: ["sistema", ...ativos("data-canal").filter((x) => x !== "sistema")]
      };
      const botao = ev.target.closest("button");
      botao.disabled = true;
      try {
        await api.put(`/instituicoes/${c.id}`, dados);
        toast("Configuração salva no banco. Aplicando...");
        setTimeout(() => location.reload(), 900);
      } catch (erro) {
        mostrarErro(root, erro.message);
        botao.disabled = false;
      }
    });
    root.querySelector("#p-cor").addEventListener("input", (ev) => {
      document.documentElement.style.setProperty("--primary", ev.target.value);
    });
  });

  // ====================================================================== LABORATÓRIO
  registrarPagina("laboratorio", async (ctx) => {
    const root = ctx.root;
    const idInst = ctx.client.id;
    const estado = { labs: [], reservas: [], formAberto: false };

    async function carregar() {
      [estado.labs, estado.reservas] = await Promise.all([
        api.get(`/laboratorios?instituicao=${idInst}`), api.get(`/reservas?instituicao=${idInst}`)]);
    }

    function formulario() {
      if (!estado.formAberto) return "";
      const livres = estado.labs.filter((l) => l.estado === "disponivel");
      if (!livres.length) return '<div class="alert">Nenhum laboratório disponível para reserva.</div>';
      const amanha = U.dataLocal(new Date(Date.now() + 86400000));
      return `<form class="crud-form" data-form><h3>Reservar laboratório</h3><div class="form-grid">
        <label class="field">Laboratório<select id="r-lab">${livres.map((l) => `<option value="${l.id}">${esc(l.nome)}</option>`).join("")}</select></label>
        <label class="field">Data<input id="r-data" type="date" required min="${U.dataLocal(new Date())}" value="${amanha}"></label>
        <label class="field">Início<input id="r-inicio" type="time" required value="08:00"></label>
        <label class="field">Fim<input id="r-fim" type="time" required value="10:00"></label>
        <label class="field">Responsável<input id="r-responsavel" maxlength="150" required value="${ctx.usuario ? esc(ctx.usuario.nome) : ""}"></label>
        <label class="field">Finalidade (opcional)<input id="r-finalidade" maxlength="150"></label></div>
        <div class="alert" data-erro hidden></div>
        <div class="form-actions"><button class="button" type="submit">Reservar</button>
          <button class="button secondary" type="button" data-acao="cancelar">Cancelar</button></div></form>`;
    }

    function render() {
      const labs = estado.labs.map((l) => `<tr><td><strong>${esc(l.nome)}</strong></td><td>${esc(l.equipamento)}</td>
        <td><span class="tag ${l.estado === "disponivel" ? "comum" : "opcional"}">${l.estado === "disponivel" ? "Disponível" : "Manutenção"}</span></td>
        <td><button class="button secondary" data-acao="estado" data-id="${l.id}" data-estado="${l.estado === "disponivel" ? "manutencao" : "disponivel"}">
          ${l.estado === "disponivel" ? "Colocar em manutenção" : "Liberar"}</button></td></tr>`).join("");
      const reservas = estado.reservas.map((r) => `<tr><td>${dataBr(r.data_reserva)}</td><td>${hora(r.hora_inicio)}–${hora(r.hora_fim)}</td>
        <td>${esc(r.laboratorio)}</td><td>${esc(r.responsavel)}${r.finalidade ? `<div class="small">${esc(r.finalidade)}</div>` : ""}</td>
        <td><button class="button danger" data-acao="cancelar-reserva" data-id="${r.id}">Cancelar</button></td></tr>`).join("");
      root.innerHTML = `<div class="lab-grid"><div class="card exclusive"><div class="row"><h3>⚗ Reserva de laboratórios</h3><span class="tag exclusivo">Somente Atlas</span></div>
          <div class="table-wrap"><table><thead><tr><th>Laboratório</th><th>Equipamento</th><th>Estado</th><th>Ação</th></tr></thead>
            <tbody>${labs || '<tr><td colspan="4" class="small">Nenhum laboratório.</td></tr>'}</tbody></table></div>
          <div class="form-actions"><button class="button" data-acao="novo">Reservar laboratório</button></div>${formulario()}
          <h3 style="margin-top:20px">Próximas reservas</h3>
          <div class="table-wrap"><table><thead><tr><th>Data</th><th>Horário</th><th>Laboratório</th><th>Responsável</th><th></th></tr></thead>
            <tbody>${reservas || '<tr><td colspan="5" class="small">Nenhuma reserva futura.</td></tr>'}</tbody></table></div></div>
        <div class="card"><h3>Como a função foi separada?</h3>
          <p>O módulo possui rotas próprias (<code>/api/laboratorios</code> e <code>/api/reservas</code>), o identificador <code>laboratorio</code> e a regra <code>clienteExclusivo: atlas</code>.</p>
          <p>A API exige o módulo ativo na instituição: em qualquer outro cliente, as rotas respondem 403. O menu só o mostra para o Instituto Atlas e apenas para professor ou administrador.</p>
          <p>Reservas que se sobrepõem no mesmo laboratório e laboratórios em manutenção são recusados pelo servidor.</p></div></div>`;
    }

    async function reservar() {
      const v = (s) => root.querySelector(s).value;
      const botao = root.querySelector("[data-form] button[type=submit]");
      botao.disabled = true;
      try {
        await api.post("/reservas", { id_laboratorio: Number(v("#r-lab")), data_reserva: v("#r-data"), hora_inicio: v("#r-inicio"),
          hora_fim: v("#r-fim"), responsavel: v("#r-responsavel"), finalidade: v("#r-finalidade") });
        toast("Reserva criada no módulo exclusivo");
        estado.formAberto = false;
        await carregar();
        render();
      } catch (erro) {
        mostrarErro(root, erro.message);
        botao.disabled = false;
      }
    }

    root.addEventListener("submit", (ev) => { ev.preventDefault(); reservar(); });
    root.addEventListener("click", async (ev) => {
      const el = ev.target.closest("[data-acao]");
      if (!el) return;
      try {
        switch (el.dataset.acao) {
          case "novo": estado.formAberto = true; render(); break;
          case "cancelar": estado.formAberto = false; render(); break;
          case "estado":
            await api.put(`/laboratorios/${el.dataset.id}`, { estado: el.dataset.estado });
            toast("Estado do laboratório atualizado");
            await carregar();
            render();
            break;
          case "cancelar-reserva":
            if (!confirm("Cancelar esta reserva?")) return;
            await api.del(`/reservas/${el.dataset.id}`);
            toast("Reserva cancelada");
            await carregar();
            render();
            break;
          default:
        }
      } catch (erro) {
        toast(erro.message);
      }
    });

    await carregar();
    render();
  });
})();
