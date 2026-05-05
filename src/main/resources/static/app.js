const apiBase = '/api';
let stompClient = null;
let currentSession = null;
let enrolledStudent = null;

function showToast(message) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.style.display = 'block';
  setTimeout(() => { toast.style.display = 'none'; }, 3200);
}

function switchTab(tabName) {
  document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));
  document.getElementById(`${tabName}Tab`).classList.add('active');
  document.querySelectorAll('.panel').forEach(panel => panel.classList.remove('active'));
  document.getElementById(`${tabName}View`).classList.add('active');
}

function createQuestionBlock(index) {
  const block = document.createElement('div');
  block.className = 'question-block';
  block.dataset.questionIndex = index;
  block.innerHTML = `
    <h4>Question ${index + 1}</h4>
    <label>Question text</label>
    <input type="text" class="question-text" placeholder="Enter the question" required />
    <div class="option-list"></div>
    <button type="button" class="secondary add-option">Add option</button>
    <label>Correct answer</label>
    <select class="correct_answer"></select>
    <button type="button" class="secondary remove-question">Remove question</button>
  `;

  const optionsContainer = block.querySelector('.option-list');
  const correctSelect = block.querySelector('.correct_answer');

  function refreshOptionSelectors() {
    correctSelect.innerHTML = '';
    const optionInputs = block.querySelectorAll('.option-input');
    optionInputs.forEach((input, optionIndex) => {
      const optionLabel = `Option ${optionIndex + 1}`;
      const optionItem = document.createElement('div');
      optionItem.className = 'option-row';
      optionItem.innerHTML = `
        <input type="text" class="option-input" placeholder="${optionLabel}" required />
        <button type="button" class="secondary remove-option">✕</button>
      `;
      optionsContainer.appendChild(optionItem);
      optionItem.querySelector('.remove-option').addEventListener('click', () => {
        if (optionsContainer.children.length <= 2) {
          showToast('Each question requires at least two options.');
          return;
        }
        optionItem.remove();
        refreshOptionSelectors();
      });
    });

    const optionInputsAfter = block.querySelectorAll('.option-input');
    optionInputsAfter.forEach((input, optionIndex) => {
      const optionName = `Option ${optionIndex + 1}`;
      correctSelect.appendChild(new Option(optionName, optionIndex));
    });
  }

  function addOptionValue(value = '') {
    const optionItem = document.createElement('div');
    optionItem.className = 'option-row';
    optionItem.innerHTML = `
      <input type="text" class="option-input" placeholder="Option ${optionsContainer.children.length + 1}" value="${value}" required />
      <button type="button" class="secondary remove-option">✕</button>
    `;
    optionsContainer.appendChild(optionItem);
    optionItem.querySelector('.remove-option').addEventListener('click', () => {
      if (optionsContainer.children.length <= 2) {
        showToast('Each question requires at least two options.');
        return;
      }
      optionItem.remove();
      refreshOptionSelectors();
    });
    refreshOptionSelectors();
  }

  block.querySelector('.add-option').addEventListener('click', () => addOptionValue(''));
  block.querySelector('.remove-question').addEventListener('click', () => {
    const wrapper = document.getElementById('questionList');
    if (wrapper.children.length <= 1) {
      showToast('At least one question is required.');
      return;
    }
    block.remove();
    rebuildQuestionTitles();
  });

  addOptionValue('');
  addOptionValue('');
  addOptionValue('');
  addOptionValue('');

  return block;
}

function rebuildQuestionTitles() {
  document.querySelectorAll('.question-block').forEach((block, index) => {
    block.querySelector('h4').textContent = `Question ${index + 1}`;
  });
}

function addQuestion() {
  const list = document.getElementById('questionList');
  const block = createQuestionBlock(list.children.length);
  list.appendChild(block);
}

function buildQuizPayload() {
  const title = document.getElementById('quizTitle').value.trim();
  const description = document.getElementById('quizDescription').value.trim();
  const questions = [];

  document.querySelectorAll('.question-block').forEach((block, index) => {
    const text = block.querySelector('.question-text').value.trim();
    const optionInputs = Array.from(block.querySelectorAll('.option-input'));
    const options = optionInputs.map(input => input.value.trim()).filter(Boolean);
    const correctAnswer = parseInt(block.querySelector('.correct_answer').value, 10);

    if (!text || options.length < 2 || options.some(option => !option)) {
      throw new Error('Please fill in all question text and option values.');
    }

    questions.push({
      text,
      options,
      correctAnswer,
      questionOrder: index
    });
  });

  return { title, description, questions };
}

async function createQuiz(event) {
  event.preventDefault();
  try {
    const payload = buildQuizPayload();
    const response = await fetch(`${apiBase}/quizzes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to create quiz');
    }

    showToast('Quiz created successfully.');
    document.getElementById('createQuizForm').reset();
    document.getElementById('questionList').innerHTML = '';
    addQuestion();
    loadQuizzes();
  } catch (error) {
    showToast(error.message);
  }
}

async function loadQuizzes() {
  const container = document.getElementById('quizList');
  container.innerHTML = '<p>Loading quizzes...</p>';

  try {
    const response = await fetch(`${apiBase}/quizzes`);
    const quizzes = await response.json();

    if (!Array.isArray(quizzes) || quizzes.length === 0) {
      container.innerHTML = '<p>No quizzes found. Create one to start a session.</p>';
      return;
    }

    container.innerHTML = quizzes.map(quiz => `
      <div class="question-block">
        <h4>${quiz.title}</h4>
        <p>${quiz.description || '<em>No description provided</em>'}</p>
        <p><strong>Questions:</strong> ${quiz.questionCount}</p>
        <button type="button" class="primary start-session" data-quiz-id="${quiz.id}">Start session</button>
      </div>
    `).join('');

    document.querySelectorAll('.start-session').forEach(button => {
      button.addEventListener('click', () => startSession(button.dataset.quizId));
    });
  } catch (error) {
    container.innerHTML = '<p>Failed to load quizzes.</p>';
  }
}

async function startSession(quizId) {
  try {
    const response = await fetch(`${apiBase}/sessions/start/${quizId}`, { method: 'POST' });
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to start session');
    }

    const session = await response.json();
    currentSession = session;
    renderSessionContext(session);
    showToast(`Session started: ${session.code}`);
  } catch (error) {
    showToast(error.message);
  }
}

function renderSessionContext(session) {
  const container = document.getElementById('sessionContext');
  const questions = session.questions || [];
  container.classList.remove('empty');
  container.innerHTML = `
    <p><strong>Session code:</strong> ${session.code}</p>
    <p><strong>Status:</strong> ${session.status}</p>
    <p><strong>Questions:</strong> ${questions.length}</p>
    <p><strong>Current question index:</strong> ${session.currentQuestionIndex + 1}</p>
    <p><strong>Participants:</strong> ${session.participantCount}</p>
  `;
}

async function joinSession(event) {
  event.preventDefault();
  const code = document.getElementById('joinSessionCode').value.trim();
  const name = document.getElementById('joinStudentName').value.trim();

  if (!code || !name) {
    showToast('Please enter both a session code and your name.');
    return;
  }

  try {
    const response = await fetch(`${apiBase}/sessions/join/${code}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Unable to join session');
    }

    const student = await response.json();
    enrolledStudent = student;
    await loadSessionByCode(code);
  } catch (error) {
    showToast(error.message);
  }
}

async function loadSessionByCode(code) {
  try {
    const response = await fetch(`${apiBase}/sessions/${code}`);
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Session not found');
    }

    const session = await response.json();
    currentSession = session;
    document.getElementById('studentSessionCard').style.display = 'block';
    document.getElementById('studentLivePanels').style.display = 'grid';
    document.getElementById('studentSessionCode').textContent = session.code;
    document.getElementById('studentIdValue').textContent = enrolledStudent.id;
    renderCurrentQuestion(session);
    connectWebSocket(session.id);
    showToast(`Joined session ${session.code}`);
  } catch (error) {
    showToast(error.message);
  }
}

function renderCurrentQuestion(session) {
  const questionTitle = document.getElementById('currentQuestionTitle');
  const optionsContainer = document.getElementById('currentQuestionOptions');
  const questionIndex = session.currentQuestionIndex || 0;
  const question = session.questions?.[questionIndex];

  if (!question) {
    questionTitle.textContent = 'No active question available.';
    optionsContainer.innerHTML = '<p>Please wait for the instructor to start the quiz.</p>';
    return;
  }

  questionTitle.textContent = question.text;
  optionsContainer.innerHTML = question.options.map((option, index) => `
    <button type="button" class="answer-button" data-option-index="${index}">${String.fromCharCode(65 + index)}. ${option}</button>
  `).join('');

  optionsContainer.querySelectorAll('.answer-button').forEach(button => {
    button.addEventListener('click', () => submitAnswer(parseInt(button.dataset.optionIndex, 10)));
  });
}

async function submitAnswer(optionIndex) {
  if (!currentSession || !enrolledStudent) {
    showToast('You must join a session first.');
    return;
  }

  const question = currentSession.questions?.[currentSession.currentQuestionIndex];
  if (!question) {
    showToast('No active question is available.');
    return;
  }

  const payload = {
    studentId: enrolledStudent.id,
    questionId: question.id,
    selectedOption: optionIndex,
    sessionCode: currentSession.code
  };

  try {
    if (stompClient && stompClient.connected) {
      stompClient.send('/app/answer', {}, JSON.stringify(payload));
      showToast('Answer submitted. Waiting for live results...');
    } else {
      const response = await fetch(`${apiBase}/answers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Unable to submit answer');
      }
      showToast('Answer submitted successfully.');
    }
  } catch (error) {
    showToast(error.message);
  }
}

function connectWebSocket(sessionId) {
  if (stompClient && stompClient.connected) {
    return;
  }

  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  stompClient.connect({}, () => {
    stompClient.subscribe(`/topic/session/${sessionId}`, message => {
      const stats = JSON.parse(message.body);
      renderStats(stats);
    });

    stompClient.subscribe(`/topic/leaderboard/${sessionId}`, message => {
      const leaderboard = JSON.parse(message.body);
      renderLeaderboard(leaderboard);
    });
  }, error => {
    console.error('WebSocket connection error', error);
    showToast('Real-time connection failed. Live updates may not work.');
  });
}

function renderStats(stats) {
  const container = document.getElementById('answerStats');
  const totals = Object.entries(stats.answerCounts || {});

  if (!totals.length) {
    container.innerHTML = '<p>No responses yet.</p>';
    return;
  }

  container.innerHTML = totals.map(([index, count]) => {
    const key = `Option ${String.fromCharCode(65 + Number(index))}`;
    const percent = (stats.answerPercentages?.[`Option ${String.fromCharCode(65 + Number(index))}`] || 0).toFixed(1);
    return `
      <div class="stats-item">
        <strong>${key}</strong>
        <div>${count} responses</div>
        <div>${percent}%</div>
      </div>
    `;
  }).join('');
}

function renderLeaderboard(leaderboard) {
  const container = document.getElementById('leaderboard');
  if (!leaderboard?.leaderboard?.length) {
    container.innerHTML = '<p>No leaderboard data yet.</p>';
    return;
  }

  container.innerHTML = leaderboard.leaderboard.map(entry => `
    <div class="leader-row">
      <strong>#${entry.rank} ${entry.name}</strong>
      <p>Correct answers: ${entry.correctAnswers}</p>
    </div>
  `).join('');
}

function bindEvents() {
  document.getElementById('instructorTab').addEventListener('click', () => switchTab('instructor'));
  document.getElementById('studentTab').addEventListener('click', () => switchTab('student'));
  document.getElementById('createQuizForm').addEventListener('submit', createQuiz);
  document.getElementById('joinSessionForm').addEventListener('submit', joinSession);
  document.getElementById('addQuestionButton').addEventListener('click', addQuestion);
}

function init() {
  bindEvents();
  addQuestion();
  loadQuizzes();
}

window.addEventListener('load', init);
