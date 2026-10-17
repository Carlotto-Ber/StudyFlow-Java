/* Funções compartilhadas pelas telas. */
window.StudyFlowUtil = (function () {
  const CANAIS = { sistema: "Sistema", email: "E-mail", app: "Aplicativo", sms: "SMS" };

  function esc(valor) {
    return String(valor == null ? "" : valor).replace(/[&<>"']/g, (c) => (
      { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]
    ));
  }

  function dataBr(iso) {
    if (!iso) return "-";
    const [ano, mes, dia] = String(iso).slice(0, 10).split("-");
    return `${dia}/${mes}/${ano}`;
  }

  function dataHoraBr(valor) {
    return valor ? `${dataBr(valor)} ${String(valor).slice(11, 16)}` : "-";
  }

  /** "14:00:00" -> "14:00" */
  function hora(valor) {
    return valor ? String(valor).slice(0, 5) : "-";
  }

  function numero(valor) {
    return Number(valor).toLocaleString("pt-BR", { maximumFractionDigits: 2 });
  }

  function pluralPalavra(palavra) {
    const p = palavra.toLowerCase();
    if (p.length <= 2) return palavra;                       // de, da, do
    if (p.endsWith("ão")) return palavra.slice(0, -2) + "ões";
    if (/[rz]$/.test(p)) return palavra + "es";
    if (p.endsWith("al")) return palavra.slice(0, -2) + "ais";
    if (p.endsWith("s")) return palavra;
    return palavra + "s";
  }

  /** "Unidade técnica" -> "Unidades técnicas"; "Componente curricular" -> "Componentes curriculares" */
  function plural(termo) {
    return String(termo).split(" ").map(pluralPalavra).join(" ");
  }

  function minutosParaTexto(minutos) {
    const h = Math.floor(minutos / 60);
    const m = minutos % 60;
    return h > 0 ? `${h}h ${String(m).padStart(2, "0")}min` : `${m}min`;
  }

  /** Date -> "AAAA-MM-DD HH:MM:SS" no horário local, como a API espera. */
  function dataHoraLocal(d) {
    const p = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
  }

  function dataLocal(d) {
    return dataHoraLocal(d).slice(0, 10);
  }

  function toast(mensagem) {
    const antigo = document.querySelector(".toast");
    if (antigo) antigo.remove();
    const el = document.createElement("div");
    el.className = "toast";
    el.textContent = mensagem;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 2600);
  }

  function cartaoErro(mensagem) {
    return `<div class="card"><h3>Não foi possível carregar os dados</h3><div class="alert">${esc(mensagem)}</div></div>`;
  }

  /** Registra uma tela. Erros de carregamento viram um cartão na própria tela. */
  function registrarPagina(nome, funcao) {
    window.StudyFlowPaginas = window.StudyFlowPaginas || {};
    window.StudyFlowPaginas[nome] = async (ctx) => {
      try {
        await funcao(ctx);
      } catch (erro) {
        ctx.root.innerHTML = cartaoErro(erro.message);
      }
    };
  }

  /** Id do estudante cujos dados a tela deve mostrar (só no perfil Estudante). */
  function estudanteAtual(ctx) {
    return ctx.roleKey === "estudante" && ctx.usuario ? ctx.usuario.id : null;
  }

  return { CANAIS, esc, dataBr, dataHoraBr, hora, numero, plural, minutosParaTexto, dataHoraLocal, dataLocal,
    toast, cartaoErro, registrarPagina, estudanteAtual };
})();
