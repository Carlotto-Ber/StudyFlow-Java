/* Cliente da API Flask. As páginas precisam ser abertas por http://localhost:5000 */
window.StudyFlowAPI = {
  base: "/api",

  async requisicao(metodo, caminho, corpo) {
    let resposta;
    try {
      resposta = await fetch(this.base + caminho, {
        method: metodo,
        headers: corpo ? { "Content-Type": "application/json" } : {},
        body: corpo ? JSON.stringify(corpo) : undefined
      });
    } catch (e) {
      throw new Error("Não foi possível conectar ao servidor. Inicie o backend (python app.py) e abra http://localhost:5000.");
    }
    const texto = await resposta.text();
    const dados = texto ? JSON.parse(texto) : null;
    if (!resposta.ok) throw new Error((dados && dados.erro) || "Erro " + resposta.status);
    return dados;
  },

  get(caminho) { return this.requisicao("GET", caminho); },
  post(caminho, corpo) { return this.requisicao("POST", caminho, corpo); },
  put(caminho, corpo) { return this.requisicao("PUT", caminho, corpo); },
  del(caminho) { return this.requisicao("DELETE", caminho); }
};
