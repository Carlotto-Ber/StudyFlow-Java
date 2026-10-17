/*
  Ponto central da variabilidade (lado do front-end).

  As INSTITUIÇÕES (nome, cor, termos, método de média, módulos ativos e canais de lembrete)
  agora vêm do banco de dados, pela rota GET /api/instituicoes, e são montadas pelo app.js.
  Aqui ficam apenas os dados de interface que não mudam por cliente:
  os perfis e a descrição de cada módulo (título, ícone, tipo e quais perfis podem acessá-lo).
*/
window.STUDYFLOW_CONFIG = {
  clientes: {}, // preenchido pelo app.js com o resultado de /api/instituicoes
  perfis: {
    estudante: "Estudante",
    professor: "Professor",
    administrador: "Administrador"
  },
  modulos: {
    dashboard: { titulo: "Painel", icone: "⌂", tipo: "comum", perfis: ["estudante", "professor", "administrador"] },
    disciplinas: { titulo: "Disciplinas", icone: "▣", tipo: "comum", perfis: ["estudante", "professor", "administrador"] },
    tarefas: { titulo: "Tarefas", icone: "✓", tipo: "comum", perfis: ["estudante", "professor", "administrador"] },
    calendario: { titulo: "Calendário", icone: "▦", tipo: "comum", perfis: ["estudante", "professor", "administrador"] },
    sessoes: { titulo: "Sessões de estudo", icone: "◷", tipo: "opcional", perfis: ["estudante", "professor"] },
    desempenho: { titulo: "Desempenho", icone: "▥", tipo: "opcional", perfis: ["estudante", "professor", "administrador"] },
    avisos: { titulo: "Avisos", icone: "♧", tipo: "opcional", perfis: ["estudante", "professor", "administrador"] },
    provas: { titulo: "Provas", icone: "▤", tipo: "opcional", perfis: ["estudante", "professor", "administrador"] },
    usuarios: { titulo: "Usuários", icone: "♙", tipo: "por perfil", perfis: ["administrador"] },
    personalizacao: { titulo: "Personalização", icone: "⚙", tipo: "por perfil", perfis: ["administrador"] },
    laboratorio: { titulo: "Laboratório inteligente", icone: "⚗", tipo: "exclusivo", clienteExclusivo: "atlas", perfis: ["professor", "administrador"] },
    arquitetura: { titulo: "Arquitetura e reuso", icone: "◇", tipo: "comum", perfis: ["estudante", "professor", "administrador"] }
  }
};
