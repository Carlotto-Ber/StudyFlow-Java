

CREATE DATABASE IF NOT EXISTS studyflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE studyflow;


-- Instituições e variabilidade

CREATE TABLE IF NOT EXISTS instituicoes (
  id_instituicao INT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(30) NOT NULL UNIQUE,
  nome VARCHAR(150) NOT NULL,
  endereco VARCHAR(255),
  telefone VARCHAR(20),
  cor VARCHAR(7) NOT NULL DEFAULT '#4f46c8',
  cor_escura VARCHAR(7) NOT NULL DEFAULT '#39319e',
  termo_disciplina VARCHAR(40) NOT NULL DEFAULT 'Disciplina',
  termo_prova VARCHAR(40) NOT NULL DEFAULT 'Prova',
  termo_aluno VARCHAR(40) NOT NULL DEFAULT 'Estudante',
  metodo_media ENUM('nenhuma','aritmetica','ponderada','conceito') NOT NULL DEFAULT 'nenhuma'
) ENGINE=InnoDB;

-- Módulos opcionais ativos em cada instituição
CREATE TABLE IF NOT EXISTS instituicao_modulos (
  id_instituicao INT NOT NULL,
  modulo VARCHAR(30) NOT NULL,
  PRIMARY KEY (id_instituicao, modulo),
  FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id_instituicao) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Canais de lembrete habilitados em cada instituição
CREATE TABLE IF NOT EXISTS instituicao_canais (
  id_instituicao INT NOT NULL,
  canal ENUM('sistema','email','app','sms') NOT NULL,
  PRIMARY KEY (id_instituicao, canal),
  FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id_instituicao) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Usuários

CREATE TABLE IF NOT EXISTS administradores (
  id_administrador INT AUTO_INCREMENT PRIMARY KEY,
  id_instituicao INT NOT NULL,
  nome VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  senha VARCHAR(255) NOT NULL,
  telefone VARCHAR(15) NOT NULL UNIQUE,
  cpf VARCHAR(14) NOT NULL UNIQUE,
  FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id_instituicao) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS professores (
  id_professor INT AUTO_INCREMENT PRIMARY KEY,
  id_instituicao INT NOT NULL,
  nome VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  senha VARCHAR(255) NOT NULL,
  telefone VARCHAR(15) NOT NULL UNIQUE,
  cpf VARCHAR(14) NOT NULL UNIQUE,
  FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id_instituicao) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS estudantes (
  id_estudante INT AUTO_INCREMENT PRIMARY KEY,
  id_instituicao INT NOT NULL,
  matricula VARCHAR(50) NOT NULL UNIQUE,
  nome VARCHAR(150) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  senha VARCHAR(255) NOT NULL,
  telefone VARCHAR(15) NOT NULL UNIQUE,
  cpf VARCHAR(14) NOT NULL UNIQUE,
  FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id_instituicao) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Disciplinas e matrículas

CREATE TABLE IF NOT EXISTS disciplinas (
  id_disciplina INT AUTO_INCREMENT PRIMARY KEY,
  id_instituicao INT NOT NULL,
  id_professor INT NULL,
  nome VARCHAR(150) NOT NULL,
  descricao TEXT,
  creditos INT NOT NULL DEFAULT 4,
  FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id_instituicao) ON DELETE CASCADE,
  FOREIGN KEY (id_professor) REFERENCES professores(id_professor) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS matriculas (
  id_estudante INT NOT NULL,
  id_disciplina INT NOT NULL,
  PRIMARY KEY (id_estudante, id_disciplina),
  FOREIGN KEY (id_estudante) REFERENCES estudantes(id_estudante) ON DELETE CASCADE,
  FOREIGN KEY (id_disciplina) REFERENCES disciplinas(id_disciplina) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Publicações (criadas pelo Factory Method)

CREATE TABLE IF NOT EXISTS tarefas (
  id_tarefa INT AUTO_INCREMENT PRIMARY KEY,
  id_disciplina INT NOT NULL,
  titulo VARCHAR(150) NOT NULL,
  descricao TEXT,
  data_entrega DATE NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_disciplina) REFERENCES disciplinas(id_disciplina) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS avisos (
  id_aviso INT AUTO_INCREMENT PRIMARY KEY,
  id_disciplina INT NOT NULL,
  titulo VARCHAR(150) NOT NULL,
  descricao TEXT NOT NULL,
  data_publicacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_disciplina) REFERENCES disciplinas(id_disciplina) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS provas (
  id_prova INT AUTO_INCREMENT PRIMARY KEY,
  id_disciplina INT NOT NULL,
  titulo VARCHAR(150) NOT NULL,
  descricao TEXT,
  data_prova DATE NOT NULL,
  peso DECIMAL(5,2) NOT NULL DEFAULT 100,       -- percentual da média (30 = 30%)
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_disciplina) REFERENCES disciplinas(id_disciplina) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Nota de cada estudante em cada prova (0 a 10)
CREATE TABLE IF NOT EXISTS notas (
  id_prova INT NOT NULL,
  id_estudante INT NOT NULL,
  nota DECIMAL(4,2) NOT NULL,
  PRIMARY KEY (id_prova, id_estudante),
  FOREIGN KEY (id_prova) REFERENCES provas(id_prova) ON DELETE CASCADE,
  FOREIGN KEY (id_estudante) REFERENCES estudantes(id_estudante) ON DELETE CASCADE
) ENGINE=InnoDB;


-- Sessões de estudo

CREATE TABLE IF NOT EXISTS sessoes_estudo (
  id_sessao INT AUTO_INCREMENT PRIMARY KEY,
  id_estudante INT NOT NULL,
  titulo VARCHAR(150) NOT NULL,
  descricao TEXT,
  data_inicio DATETIME NOT NULL,
  data_fim DATETIME NULL,
  FOREIGN KEY (id_estudante) REFERENCES estudantes(id_estudante) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sessao_disciplinas (
  id_sessao INT NOT NULL,
  id_disciplina INT NOT NULL,
  PRIMARY KEY (id_sessao, id_disciplina),
  FOREIGN KEY (id_sessao) REFERENCES sessoes_estudo(id_sessao) ON DELETE CASCADE,
  FOREIGN KEY (id_disciplina) REFERENCES disciplinas(id_disciplina) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Laboratório inteligente

CREATE TABLE IF NOT EXISTS laboratorios (
  id_laboratorio INT AUTO_INCREMENT PRIMARY KEY,
  id_instituicao INT NOT NULL,
  nome VARCHAR(100) NOT NULL,
  equipamento VARCHAR(150) NOT NULL,
  estado ENUM('disponivel','manutencao') NOT NULL DEFAULT 'disponivel',
  FOREIGN KEY (id_instituicao) REFERENCES instituicoes(id_instituicao) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservas_laboratorio (
  id_reserva INT AUTO_INCREMENT PRIMARY KEY,
  id_laboratorio INT NOT NULL,
  responsavel VARCHAR(150) NOT NULL,
  finalidade VARCHAR(150),
  data_reserva DATE NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fim TIME NOT NULL,
  FOREIGN KEY (id_laboratorio) REFERENCES laboratorios(id_laboratorio) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabelas alimentadas pelos Observers

CREATE TABLE IF NOT EXISTS notificacoes (
  id_notificacao INT AUTO_INCREMENT PRIMARY KEY,
  id_estudante INT NOT NULL,
  canal ENUM('sistema','email','app','sms') NOT NULL,
  tipo ENUM('tarefa','aviso','prova') NOT NULL,
  id_referencia INT NOT NULL,                   -- id da tarefa, aviso ou prova
  evento ENUM('criado','alterado') NOT NULL,
  mensagem VARCHAR(500) NOT NULL,
  lida TINYINT(1) NOT NULL DEFAULT 0,
  criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_estudante) REFERENCES estudantes(id_estudante) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS eventos_calendario (
  id_evento INT AUTO_INCREMENT PRIMARY KEY,
  id_estudante INT NOT NULL,
  tipo ENUM('tarefa','prova') NOT NULL,
  id_tarefa INT NULL,
  id_prova INT NULL,
  titulo VARCHAR(170) NOT NULL,
  data_evento DATE NOT NULL,
  UNIQUE (id_estudante, id_tarefa),
  UNIQUE (id_estudante, id_prova),
  FOREIGN KEY (id_estudante) REFERENCES estudantes(id_estudante) ON DELETE CASCADE,
  FOREIGN KEY (id_tarefa) REFERENCES tarefas(id_tarefa) ON DELETE CASCADE,
  FOREIGN KEY (id_prova) REFERENCES provas(id_prova) ON DELETE CASCADE
) ENGINE=InnoDB;
