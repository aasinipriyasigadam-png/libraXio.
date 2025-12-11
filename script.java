// Simple book app with search, genre filter, add-book form, and localStorage persistence.

const SAMPLE_KEY = 'allbooks_sample_v1';

const sampleBooks = [
  {
    id: cryptoRandomId(),
    title: "The Little Prince",
    author: "Antoine de Saint-Exupéry",
    genre: "Children's Fiction",
    description: "A poetic tale about a pilot stranded in the desert and a little prince from another planet.",
    cover: "https://upload.wikimedia.org/wikipedia/en/3/30/Littleprince.JPG"
  },
  {
    id: cryptoRandomId(),
    title: "Sapiens: A Brief History of Humankind",
    author: "Yuval Noah Harari",
    genre: "History",
    description: "A sweeping exploration of human history, cognition, and society.",
    cover: ""
  },
  {
    id: cryptoRandomId(),
    title: "A Brief History of Time",
    author: "Stephen Hawking",
    genre: "Science",
    description: "An accessible introduction to cosmology and the origin of the universe.",
    cover: ""
  },
  {
    id: cryptoRandomId(),
    title: "Becoming",
    author: "Michelle Obama",
    genre: "Biography",
    description: "A memoir by the former First Lady of the United States.",
    cover: ""
  },
  {
    id: cryptoRandomId(),
    title: "The Hobbit",
    author: "J.R.R. Tolkien",
    genre: "Fantasy",
    description: "Bilbo Baggins goes on a grand adventure with dwarves and a dragon.",
    cover: ""
  },
  {
    id: cryptoRandomId(),
    title: "Clean Code",
    author: "Robert C. Martin",
    genre: "Programming",
    description: "A handbook of agile software craftsmanship and best practices for writing clean code.",
    cover: ""
  }
];

// Utilities
function cryptoRandomId() {
  return Math.random().toString(36).slice(2, 10);
}
function qs(sel) { return document.querySelector(sel); }
function qsa(sel) { return Array.from(document.querySelectorAll(sel)); }
function saveBooks(arr){
  localStorage.setItem(SAMPLE_KEY, JSON.stringify(arr));
}
function loadBooks(){
  const raw = localStorage.getItem(SAMPLE_KEY);
  if(!raw) {
    saveBooks(sampleBooks);
    return sampleBooks.slice();
  }
  try{
    return JSON.parse(raw);
  }catch(e){
    console.error("Can't parse books from storage, resetting.", e);
    saveBooks(sampleBooks);
    return sampleBooks.slice();
  }
}

// Rendering
const booksSection = qs('#booksSection');
const searchInput = qs('#searchInput');
const genreFilter = qs('#genreFilter');

let books = loadBooks();

function renderGenreOptions() {
  const genres = Array.from(new Set(books.map(b => (b.genre || 'Unknown').trim()))).sort();
  // clear existing (keep first 'all')
  genreFilter.innerHTML = '<option value="all">All genres</option>';
  genres.forEach(g => {
    const opt = document.createElement('option');
    opt.value = g;
    opt.textContent = g;
    genreFilter.appendChild(opt);
  });
}

function highlight(text, query){
  if(!query) return escapeHtml(text);
  const q = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const re = new RegExp((${q}), 'ig');
  return escapeHtml(text).replace(re, '<mark>$1</mark>');
}
function escapeHtml(s){
  return String(s)
    .replace(/&/g,'&amp;')
    .replace(/</g,'&lt;')
    .replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;');
}

function renderBooks() {
  const q = searchInput.value.trim();
  const genre = genreFilter.value;
  booksSection.innerHTML = '';

  // filter by genre
  let list = books.slice();
  if(genre && genre !== 'all'){
    list = list.filter(b => (b.genre || '').toLowerCase() === genre.toLowerCase());
  }

  // search by title/author/description
  if(q){
    const low = q.toLowerCase();
    list = list.filter(b =>
      (b.title || '').toLowerCase().includes(low) ||
      (b.author || '').toLowerCase().includes(low) ||
      (b.description || '').toLowerCase().includes(low) ||
      (b.genre || '').toLowerCase().includes(low)
    );
  }

  if(list.length === 0){
    const empty = document.createElement('div');
    empty.className = 'card';
    empty.innerHTML = <div class="card-body"><h3>No results</h3><p class="meta">Try changing filters or add a new book.</p></div>;
    booksSection.appendChild(empty);
    return;
  }

  list.forEach(book => {
    const card = document.createElement('article');
    card.className = 'card';
    const coverDiv = document.createElement('div');
    coverDiv.className = 'cover';
    const img = document.createElement('img');
    img.alt = book.title + " cover";
    img.src = book.cover || placeholderFor(book.title);
    img.onerror = function(){ this.src = placeholderFor(book.title); }
    coverDiv.appendChild(img);

    const body = document.createElement('div');
    body.className = 'card-body';

    const titleHtml = highlight(book.title || 'Untitled', q);
    const authorHtml = highlight(book.author || 'Unknown author', q);
    const genreHtml = escapeHtml(book.genre || 'Uncategorized');
    const descHtml = highlight(book.description || '', q);

    body.innerHTML = `<h3>${titleHtml}</h3>
                      <div class="meta">${authorHtml} • <em>${genreHtml}</em></div>
                      <p class="desc">${descHtml}</p>`;

    card.appendChild(coverDiv);
    card.appendChild(body);
    booksSection.appendChild(card);
  });
}

function placeholderFor(title){
  // simple SVG data URI as placeholder cover
  const txt = (title||'Book').slice(0,18);
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='300' height='420'>
    <defs><linearGradient id='g' x1='0' x2='1'><stop offset='0' stop-color='#ffe29f'/><stop offset='1' stop-color='#ffa99f'/></linearGradient></defs>
    <rect width='100%' height='100%' fill='url(#g)' rx='12' />
    <text x='50%' y='52%' font-size='28' text-anchor='middle' fill='#423a3a' font-family='Arial, Helvetica, sans-serif'>${escapeXml(txt)}</text>
  </svg>`;
  return 'data:image/svg+xml;utf8,' + encodeURIComponent(svg);
}
function escapeXml(s){ return String(s).replace(/[&<>]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;'}[c])); }

// Form & Modal handling
const modal = qs('#modal');
const addBookBtn = qs('#addBookBtn');
const closeModal = qs('#closeModal');
const cancelBtn = qs('#cancelBtn');
const bookForm = qs('#bookForm');

addBookBtn.addEventListener('click', () => openModal());
closeModal.addEventListener('click', closeModalFn);
cancelBtn.addEventListener('click', closeModalFn);
modal.addEventListener('click', (e) => { if(e.target === modal) closeModalFn(); });

function openModal(){
  modal.setAttribute('aria-hidden','false');
  modal.style.display = 'flex';
  setTimeout(()=> modal.focus(), 160);
}
function closeModalFn(){
  modal.setAttribute('aria-hidden','true');
  setTimeout(()=> { modal.style.display = 'none'; }, 180);
  bookForm.reset();
}

bookForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const data = new FormData(bookForm);
  const newBook = {
    id: cryptoRandomId(),
    title: (data.get('title') || '').trim(),
    author: (data.get('author') || '').trim(),
    genre: (data.get('genre') || '').trim(),
    description: (data.get('description') || '').trim(),
    cover: (data.get('cover') || '').trim()
  };
  books.unshift(newBook); // put newest first
  saveBooks(books);
  renderGenreOptions();
  renderBooks();
  closeModalFn();
});

// Search & filter
searchInput.addEventListener('input', () => {
  renderBooks();
});
genreFilter.addEventListener('change', () => {
  renderBooks();
});

// initial render
renderGenreOptions();
renderBooks();

// Accessibility: close modal on Escape
document.addEventListener('keydown', (e) => {
  if(e.key === 'Escape' && modal.getAttribute('aria-hidden') === 'false') closeModalFn();
});
