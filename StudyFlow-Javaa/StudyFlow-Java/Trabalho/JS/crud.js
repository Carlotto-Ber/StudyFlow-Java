/*
  Telas CRUD de Tarefas, Avisos e Provas, ligadas ao backend Java/MySQL.
  Todas usam a mesma lógica: o que muda é só a configuração de cada recurso.
  Provas ainda têm o lançamento de notas (professor/administrador) e a coluna de nota do estudante.
*/
(function () {
  const api = window.StudyFlowAPI;
  const U = window.StudyFlowUtil;
  const { esc, dataBr, dataHoraBr, toast, CANAIS } = U;

  function configurar(recurso, ctx) {
    const termos = ctx.client.termos;
    const prova = termos.prova.toLowerCase();
    return {
      tarefas: {
        titulo: "Tarefas",
        singular: "tarefa",
        novo: "+ Nova tarefa",
        msgSalvo: "Tarefa salva com sucesso",
        msgExcluido: "Tarefa excluída",
        confirmaExcluir: "Excluir esta tarefa?",
        tipoNotificacao: "tarefa",
        tipoEvento: "tarefa",
        descricaoObrigatoria: false,
        campos: [{ chave: "data_entrega", rotulo: "Data de entrega", tipo: "date" }],
        colunas: [{ rotulo: "Entrega", valor: (i) => dataBr(i.data_entrega) }],
        textoCalendario: "Ao criar uma tarefa ou mudar o prazo, o evento de cada estudante matriculado é criado ou atualizado.",
        podeEditar: ctx.roleKey !== "estudante" || ctx.clientKey === "individual"
      },
      avisos: {
        titulo: "Avisos",
        singular: "aviso",
        novo: "+ Novo aviso",
        msgSalvo: "Aviso salvo com sucesso",
        msgExcluido: "Aviso excluído",
        confirmaExcluir: "Excluir este aviso?",
        tipoNotificacao: "aviso",
        tipoEvento: null,
        descricaoObrigatoria: true,
        campos: [],
        colunas: [{ rotulo: "Publicado em", valor: (i) => dataHoraBr(i.data_publicacao) }],
        podeEditar: ctx.roleKey !== "estudante"
      },
      provas: {
        titulo: U.plural(termos.prova),
        singular: prova,
        novo: `+ Adicionar ${prova}`,
        msgSalvo: "Alterações salvas",
        msgExcluido: "Registro excluído",
        confirmaExcluir: "Excluir este registro?",
        tipoNotificacao: "prova",
        tipoEvento: "prova",
        descricaoObrigatoria: false,
        campos: [
          { chave: "data_prova", rotulo: "Data", tipo: "date" },
          { chave: "peso", rotulo: "Peso (%)", tipo: "number", min: 1, max: 100, step: "1", padrao: 100 }
        ],
        colunas: [
          { rotulo: "Data", valor: (i) => dataBr(i.data_prova) },
          { rotulo: "Peso", valor: (i) => U.numero(i.peso) + "%" }
        ],
        textoCalendario: `Ao criar ${prova === "prova" ? "uma prova" : "um registro"} ou mudar a data, o evento de cada estudante matriculado é criado ou atualizado.`,
        notas: true,
        podeEditar: ctx.roleKey !== "estudante"
      }
    }[recurso];
  }

  async function montar(recurso, ctx) {
    const cfg = configurar(recurso, ctx);
    const root = ctx.root;
    const termoDisciplina = ctx.client.termos.disciplina;
    const estudante = U.estudanteAtual(ctx);
    const qEst = estudante ? `&estudante=${estudante}` : "";
    const idInst = ctx.client.id;
    const estado = {
      disciplinas: [], itens: [], notificacoes: [], eventos: [], notasDoEstudante: {},
      formAberto: false, editando: null, painelNotas: null
    };

    async function carregar() {
      const [itens, notificacoes, eventos, notas] = await Promise.all([
        api.get(`/${recurso}?instituicao=${idInst}${qEst}`),
        api.get(`/notificacoes?instituicao=${idInst}&tipo=${cfg.tipoNotificacao}${qEst}`),
        cfg.tipoEvento ? api.get(`/eventos?instituicao=${idInst}&tipo=${cfg.tipoEvento}${qEst}`) : Promise.resolve([]),
        cfg.notas && estudante ? api.get(`/notas?estudante=${estudante}`) : Promise.resolve([])
      ]);
      Object.assign(estado, { itens, notificacoes, eventos });
      estado.notasDoEstudante = {};
      notas.forEach((n) => { estado.notasDoEstudante[n.id_prova] = n.rotulo; });
    }

    // ---------------------------------------------------------------- formulário
    function formulario() {
      if (!estado.formAberto) return "";
      if (!estado.disciplinas.length) {
        return `<div class="alert">Nenhuma ${esc(termoDisciplina.toLowerCase())} disponível. Cadastre uma na tela de ${esc(U.plural(termoDisciplina))}.</div>`;
      }
      const e = estado.editando;
      const opcoes = estado.disciplinas.map((d) =>
        `<option value="${d.id}" ${e && e.id_disciplina === d.id ? "selected" : ""}>${esc(d.nome)}</option>`).join("");
      const extras = cfg.campos.map((c) => {
        const valor = e ? (c.tipo === "date" ? String(e[c.chave]).slice(0, 10) : Number(e[c.chave])) : (c.padrao != null ? c.padrao : "");
        const atributos = c.tipo === "number" ? `min="${c.min}" max="${c.max}" step="${c.step}"` : "";
        return `<label class="field">${esc(c.rotulo)}
          <input id="f-${c.chave}" type="${c.tipo}" required ${atributos} value="${esc(valor)}"></label>`;
      }).join("");
      return `<form class="crud-form" data-form>
        <h3>${e ? "Editar" : "Cadastro de"} ${esc(cfg.singular)}</h3>
        <div class="form-grid">
          <label class="field">${esc(termoDisciplina)}
            <select id="f-disciplina" ${e ? "disabled" : ""}>${opcoes}</select></label>
          ${extras}
          <label class="field wide">Título
            <input id="f-titulo" maxlength="150" required value="${e ? esc(e.titulo) : ""}"></label>
          <label class="field wide">Descrição${cfg.descricaoObrigatoria ? "" : " (opcional)"}
            <textarea id="f-descricao" rows="3" ${cfg.descricaoObrigatoria ? "required" : ""}>${e ? esc(e.descricao || "") : ""}</textarea></label>
        </div>
        <div class="alert" data-erro hidden></div>
        <div class="form-actions">
          <button class="button" type="submit">Salvar</button>
          <button class="button secondary" type="button" data-acao="cancelar">Cancelar</button>
        </div>
      </form>`;
    }

    // ---------------------------------------------------------------- tabela
    function tabela() {
      const comNota = cfg.notas && estudante;
      const colunas = ["Título", termoDisciplina, ...cfg.colunas.map((c) => c.rotulo)];
      if (comNota) colunas.push("Nota/Conceito");
      const acoes = cfg.podeEditar;
      const cabecalho = colunas.map((c) => `<th>${esc(c)}</th>`).join("") + (acoes ? "<th>Ações</th>" : "");
      const linhas = estado.itens.map((i) => {
        const botoes = acoes ? `<td><div class="form-actions">
            ${cfg.notas ? `<button class="button secondary" data-acao="notas" data-id="${i.id}">Notas</button>` : ""}
            <button class="button secondary" data-acao="editar" data-id="${i.id}">Editar</button>
            <button class="button danger" data-acao="excluir" data-id="${i.id}">Excluir</button>
          </div></td>` : "";
        return `<tr>
          <td><strong>${esc(i.titulo)}</strong>${i.descricao ? `<div class="small">${esc(i.descricao)}</div>` : ""}</td>
          <td>${esc(i.disciplina_nome)}</td>
          ${cfg.colunas.map((c) => `<td>${c.valor(i)}</td>`).join("")}
          ${comNota ? `<td>${estado.notasDoEstudante[i.id] ? `<strong>${esc(estado.notasDoEstudante[i.id])}</strong>` : '<span class="small">—</span>'}</td>` : ""}
          ${botoes}</tr>`;
      }).join("");
      const vazio = `<tr><td colspan="${colunas.length + (acoes ? 1 : 0)}" class="small">Nenhum registro cadastrado.</td></tr>`;
      return `<div class="table-wrap"><table><thead><tr>${cabecalho}</tr></thead><tbody>${linhas || vazio}</tbody></table></div>`;
    }

    // ---------------------------------------------------------------- notas (provas)
    function painelNotas() {
      if (!estado.painelNotas) return "";
      const { prova, linhas } = estado.painelNotas;
      const corpo = linhas.map((l) => `<tr>
        <td>${esc(l.nome)}</td>
        <td><input class="nota-input" id="nota-${l.id_estudante}" type="number" min="0" max="10" step="0.1"
             value="${l.nota == null ? "" : l.nota}" placeholder="0 a 10"></td>
        <td>${l.rotulo ? `<span class="tag ativo">${esc(l.rotulo)}</span>` : '<span class="small">sem nota</span>'}</td>
        <td><button class="button secondary" data-acao="salvar-nota" data-estudante="${l.id_estudante}">Salvar</button></td></tr>`).join("");
      return `<div class="card" style="margin-top:16px">
        <div class="row"><h3>Lançar notas — ${esc(prova.titulo)}</h3><button class="button secondary" data-acao="fechar-notas">Fechar</button></div>
        <p class="small">Regra de resultado desta instituição: ${esc(ctx.client.media)}. Deixe o campo vazio e salve para remover a nota.</p>
        <div class="table-wrap"><table><thead><tr><th>${esc(ctx.client.termos.aluno)}</th><th>Nota</th><th>Resultado</th><th></th></tr></thead>
        <tbody>${corpo || '<tr><td colspan="4" class="small">Nenhum estudante matriculado nesta disciplina.</td></tr>'}</tbody></table></div></div>`;
    }

    // ---------------------------------------------------------------- painéis dos observers
    function painelNotificacoes() {
      const canais = ctx.client.canais.map((c) => `<span class="tag ativo">${CANAIS[c] || esc(c)}</span>`).join(" ");
      const linhas = estado.notificacoes.map((n) => `<tr>
        <td>${esc(n.estudante)}</td><td><span class="tag">${CANAIS[n.canal] || esc(n.canal)}</span></td>
        <td>${esc(n.evento)}</td><td>${esc(n.mensagem)}</td></tr>`).join("");
      return `<div class="card"><h3>${estudante ? "Minhas notificações" : "Notificações geradas"} (Observer)</h3>
        <p>Canais ativos em ${esc(ctx.client.nome)}: ${canais}</p>
        <div class="table-wrap"><table><thead><tr><th>${esc(ctx.client.termos.aluno)}</th><th>Canal</th><th>Evento</th><th>Mensagem</th></tr></thead>
        <tbody>${linhas || '<tr><td colspan="4" class="small">Nenhuma notificação ainda.</td></tr>'}</tbody></table></div></div>`;
    }

    function painelLateral() {
      if (cfg.tipoEvento) {
        const linhas = estado.eventos.map((ev) => `<tr><td>${esc(ev.estudante)}</td><td>${dataBr(ev.data_evento)}</td><td>${esc(ev.titulo)}</td></tr>`).join("");
        return `<div class="card"><h3>${estudante ? "Meu calendário" : "Calendário dos estudantes"} (Observer)</h3>
          <p>${esc(cfg.textoCalendario)}</p>
          <div class="table-wrap"><table><thead><tr><th>${esc(ctx.client.termos.aluno)}</th><th>Data</th><th>Evento</th></tr></thead>
          <tbody>${linhas || '<tr><td colspan="3" class="small">Nenhum evento ainda.</td></tr>'}</tbody></table></div></div>`;
      }
      const modulos = ctx.client.modulos.filter((m) => !["dashboard", "disciplinas", "tarefas", "calendario", "arquitetura"].includes(m))
        .map((m) => `<span class="tag opcional">${esc(m)}</span>`).join(" ") || "nenhum";
      return `<div class="card"><h3>Variabilidade desta instituição</h3>
        <p>O módulo <code>avisos</code> só existe para instituições que o ativaram. A API recusa a criação em quem não tem o módulo.</p>
        <p><strong>Módulos opcionais ativos:</strong> ${modulos}</p>
        <p>Os canais ao lado vêm de <code>instituicao_canais</code> e podem ser alterados na tela de Personalização.</p></div>`;
    }

    function render() {
      root.innerHTML = `<div class="card">
          <div class="row"><div><h3>${esc(cfg.titulo)} — ${esc(ctx.client.nome)}</h3>
            <span class="small">${cfg.podeEditar ? "Crie, edite e exclua. Cada alteração dispara os observers." : "Somente leitura para este perfil."}</span></div>
            ${cfg.podeEditar ? `<button class="button" data-acao="novo">${esc(cfg.novo)}</button>` : ""}</div>
          ${formulario()}
          ${tabela()}
          ${cfg.notas ? `<p><strong>Regra de resultado:</strong> ${esc(ctx.client.media)}.</p>` : ""}
        </div>
        ${painelNotas()}
        <div class="grid two" style="margin-top:16px">${painelNotificacoes()}${painelLateral()}</div>`;
    }

    function mostrarErro(mensagem) {
      const alvo = root.querySelector("[data-erro]");
      if (alvo) { alvo.textContent = mensagem; alvo.hidden = false; }
    }

    async function salvar() {
      const valor = (seletor) => root.querySelector(seletor).value;
      const dados = { titulo: valor("#f-titulo"), descricao: valor("#f-descricao") };
      cfg.campos.forEach((c) => {
        dados[c.chave] = c.tipo === "number" ? Number(valor(`#f-${c.chave}`)) : valor(`#f-${c.chave}`);
      });
      if (!estado.editando) dados.id_disciplina = Number(valor("#f-disciplina"));
      const botao = root.querySelector("[data-form] button[type=submit]");
      botao.disabled = true;
      try {
        if (estado.editando) await api.put(`/${recurso}/${estado.editando.id}`, dados);
        else await api.post(`/${recurso}`, dados);
        toast(cfg.msgSalvo);
        estado.formAberto = false;
        estado.editando = null;
        await carregar();
        render();
      } catch (erro) {
        mostrarErro(erro.message);
        botao.disabled = false;
      }
    }

    async function abrirNotas(id) {
      const prova = estado.itens.find((i) => i.id === id);
      const linhas = await api.get(`/provas/${id}/notas`);
      estado.painelNotas = { prova, linhas };
      render();
      root.querySelector(".nota-input")?.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    async function salvarNota(idEstudante) {
      const campo = root.querySelector(`#nota-${idEstudante}`);
      const nota = campo.value === "" ? null : Number(campo.value);
      const linhas = await api.put(`/provas/${estado.painelNotas.prova.id}/notas`, { id_estudante: idEstudante, nota });
      estado.painelNotas.linhas = linhas;
      render();
      toast("Nota salva");
    }

    root.addEventListener("submit", (ev) => { ev.preventDefault(); salvar(); });

    root.addEventListener("click", async (ev) => {
      const el = ev.target.closest("[data-acao]");
      if (!el) return;
      const id = Number(el.dataset.id);
      try {
        switch (el.dataset.acao) {
          case "novo":
            Object.assign(estado, { formAberto: true, editando: null });
            render();
            root.querySelector("#f-titulo")?.focus();
            break;
          case "editar":
            Object.assign(estado, { formAberto: true, editando: estado.itens.find((i) => i.id === id) });
            render();
            break;
          case "cancelar":
            Object.assign(estado, { formAberto: false, editando: null });
            render();
            break;
          case "excluir":
            if (!confirm(cfg.confirmaExcluir)) return;
            await api.del(`/${recurso}/${id}`);
            toast(cfg.msgExcluido);
            if (estado.painelNotas && estado.painelNotas.prova.id === id) estado.painelNotas = null;
            await carregar();
            render();
            break;
          case "notas":
            await abrirNotas(id);
            break;
          case "fechar-notas":
            estado.painelNotas = null;
            render();
            break;
          case "salvar-nota":
            await salvarNota(Number(el.dataset.estudante));
            break;
          default:
        }
      } catch (erro) {
        toast(erro.message);
      }
    });

    estado.disciplinas = await api.get(`/disciplinas?instituicao=${idInst}${qEst}`);
    await carregar();
    render();
  }

  ["tarefas", "avisos", "provas"].forEach((recurso) => U.registrarPagina(recurso, (ctx) => montar(recurso, ctx)));
})();
