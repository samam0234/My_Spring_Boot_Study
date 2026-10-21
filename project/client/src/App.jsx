import { useEffect, useState } from "react";
import { api, getToken, setToken } from "./api";

const NAV = [
  ["home", "홈"],
  ["board", "게시판"],
  ["messages", "쪽지"],
  ["explore", "둘러보기"],
  ["desk", "작업실"],
];

function splitTags(value) {
  return value.split(",").map((tag) => tag.trim()).filter(Boolean);
}

function ErrorNote({ error }) {
  if (!error) return null;
  return <p className="error">{error}</p>;
}

function Home({ onWrite }) {
  const [hello, setHello] = useState("");
  const [time, setTime] = useState(null);
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;
    Promise.all([api.hello(), api.time(), api.profile()])
      .then(([helloText, timeStatus, profileStatus]) => {
        if (!alive) return;
        setHello(typeof helloText === "string" ? helloText : JSON.stringify(helloText));
        setTime(timeStatus);
        setProfile(profileStatus);
      })
      .catch((err) => alive && setError(err.message));
    return () => { alive = false; };
  }, []);

  return (
    <>
      <section className="hero">
        <div>
          <p className="muted">Spring Boot demo · React client</p>
          <h1>오늘의 글을 남기는 작은 게시판.</h1>
          <p>회원가입과 쪽지, 태그 검색, 날씨와 시세는 모두 `demo` API를 부릅니다. 화면 코드는 `project/client`에만 있습니다.</p>
          <div className="row">
            <button className="primary" onClick={onWrite}>글 쓰기</button>
          </div>
        </div>
        <aside className={time?.night ? "panel night" : "panel"}>
          <h2>{time ? time.period : "시간"}</h2>
          <p>{time?.message || "서버에 시각을 물어보는 중입니다."}</p>
          <p className="meta">{time?.currentTime}</p>
          <div className="chips">
            <span className="chip">{hello || "hello"}</span>
            <span className="chip">{profile?.activeProfiles?.join(", ") || "profile"}</span>
          </div>
          <ErrorNote error={error} />
        </aside>
      </section>
    </>
  );
}

function Board({ onOpen }) {
  const [page, setPage] = useState(null);
  const [q, setQ] = useState("");
  const [draft, setDraft] = useState("");
  const [error, setError] = useState("");
  const [pageIndex, setPageIndex] = useState(0);

  async function load(nextPage, keyword) {
    try {
      setError("");
      setPage(await api.posts({ page: nextPage, size: 5, q: keyword }));
      setPageIndex(nextPage);
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => { load(0, ""); }, []);

  return (
    <div className="layout">
      <section className="list">
        <form className="row" onSubmit={(event) => { event.preventDefault(); load(0, draft); setQ(draft); }}>
          <input value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="제목, 본문, 작성자, 태그" />
          <button className="primary" type="submit">검색</button>
        </form>
        <ErrorNote error={error} />
        {(page?.content || []).map((post) => (
          <button className="card" key={post.id} onClick={() => onOpen(post.id)}>
            <h2>{post.title}</h2>
            <p>{post.content}</p>
            <div className="tags">
              <span className="meta">{post.author}</span>
              {(post.tags || []).map((tag) => <span className="tag" key={tag}>{tag}</span>)}
            </div>
          </button>
        ))}
        <div className="pager">
          <button className="ghost" disabled={pageIndex === 0} onClick={() => load(pageIndex - 1, q)}>이전</button>
          <span className="meta">{page ? `${page.page + 1} / ${Math.max(page.totalPages, 1)}` : ""}</span>
          <button className="ghost" disabled={!page?.hasNext} onClick={() => load(pageIndex + 1, q)}>다음</button>
        </div>
      </section>
      <Composer onSaved={() => load(0, q)} />
    </div>
  );
}

function Composer({ onSaved, initial, onDone }) {
  const [title, setTitle] = useState(initial?.title || "");
  const [content, setContent] = useState(initial?.content || "");
  const [author, setAuthor] = useState(initial?.author || "");
  const [tags, setTags] = useState((initial?.tags || []).join(", "));
  const [error, setError] = useState("");

  async function submit(event) {
    event.preventDefault();
    const body = { title, content, author, tags: splitTags(tags) };
    try {
      setError("");
      if (initial?.id) {
        await api.updatePost(initial.id, { title, content, tags: splitTags(tags) });
        onDone?.();
      } else {
        await api.createPost(body);
        setTitle("");
        setContent("");
        setTags("");
        onSaved?.();
      }
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <form className="composer panel" onSubmit={submit}>
      <h2>{initial ? "고치기" : "새 글"}</h2>
      <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="제목" required />
      <textarea value={content} onChange={(event) => setContent(event.target.value)} placeholder="내용" required />
      {!initial && <input value={author} onChange={(event) => setAuthor(event.target.value)} placeholder="작성자" required />}
      <input value={tags} onChange={(event) => setTags(event.target.value)} placeholder="태그, 쉼표로 구분" />
      <ErrorNote error={error} />
      <button className="primary" type="submit">{initial ? "저장" : "올리기"}</button>
    </form>
  );
}

function PostView({ id, onBack }) {
  const [post, setPost] = useState(null);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState("");

  async function load() {
    try {
      setPost(await api.post(id));
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => { load(); }, [id]);

  async function remove() {
    try {
      await api.deletePost(id);
      onBack();
    } catch (err) {
      setError(err.message);
    }
  }

  if (!post) return <section className="panel"><ErrorNote error={error || "글을 불러오는 중입니다."} /></section>;

  return (
    <section className="panel">
      <button className="ghost" onClick={onBack}>목록</button>
      <h2>{post.title}</h2>
      <p className="meta">{post.author} · {(post.tags || []).join(" · ")}</p>
      <p style={{ whiteSpace: "pre-wrap" }}>{post.content}</p>
      <div className="row">
        <button className="ghost" onClick={() => setEditing((value) => !value)}>수정</button>
        <button className="ghost danger" onClick={remove}>삭제</button>
      </div>
      <ErrorNote error={error} />
      {editing && <Composer initial={post} onDone={() => { setEditing(false); load(); }} />}
    </section>
  );
}

function AuthBox({ onChange }) {
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function submit(event) {
    event.preventDefault();
    try {
      setError("");
      setNotice("");
      if (mode === "signup") {
        await api.signup({ username, password });
        setNotice("가입되었습니다. 로그인하세요.");
        setMode("login");
        return;
      }
      const result = await api.login({ username, password });
      setToken(result.token);
      onChange(true);
    } catch (err) {
      setError(err.message);
    }
  }

  if (getToken()) {
    return (
      <section className="panel">
        <h2>로그인됨</h2>
        <p className="meta">토큰은 이 브라우저의 localStorage에 있습니다.</p>
        <button className="ghost" onClick={() => { setToken(null); onChange(false); }}>나가기</button>
      </section>
    );
  }

  return (
    <form className="panel composer" onSubmit={submit}>
      <h2>{mode === "login" ? "입장" : "가입"}</h2>
      <input value={username} onChange={(event) => setUsername(event.target.value)} placeholder="아이디" required />
      <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="비밀번호" required />
      <ErrorNote error={error} />
      {notice && <p className="meta">{notice}</p>}
      <div className="row">
        <button className="primary" type="submit">{mode === "login" ? "로그인" : "가입하기"}</button>
        <button className="ghost" type="button" onClick={() => setMode(mode === "login" ? "signup" : "login")}>
          {mode === "login" ? "계정이 없습니다" : "이미 있습니다"}
        </button>
      </div>
    </form>
  );
}

function Messages({ authed }) {
  const [box, setBox] = useState("inbox");
  const [items, setItems] = useState([]);
  const [receiver, setReceiver] = useState("");
  const [content, setContent] = useState("");
  const [error, setError] = useState("");

  async function load(nextBox) {
    try {
      setError("");
      setItems(nextBox === "sent" ? await api.sent() : await api.inbox());
      setBox(nextBox);
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => { if (authed) load("inbox"); }, [authed]);

  if (!authed) return <section className="panel"><h2>쪽지</h2><p>로그인 뒤에 보낼 수 있습니다.</p></section>;

  return (
    <div className="layout">
      <section className="list">
        <div className="row">
          <button className="ghost" onClick={() => load("inbox")}>받은 쪽지</button>
          <button className="ghost" onClick={() => load("sent")}>보낸 쪽지</button>
        </div>
        <ErrorNote error={error} />
        {items.map((item) => (
          <article className="card" key={item.id}>
            <h2>{box === "sent" ? item.receiverUsername : item.senderUsername}</h2>
            <p>{item.content}</p>
          </article>
        ))}
        {items.length === 0 && <p className="meta">쪽지가 없습니다.</p>}
      </section>
      <form className="composer panel" onSubmit={async (event) => {
        event.preventDefault();
        try {
          await api.sendMessage({ receiverUsername: receiver, content });
          setContent("");
          load(box);
        } catch (err) {
          setError(err.message);
        }
      }}>
        <h2>보내기</h2>
        <input value={receiver} onChange={(event) => setReceiver(event.target.value)} placeholder="받는 아이디" required />
        <textarea value={content} onChange={(event) => setContent(event.target.value)} placeholder="내용" required />
        <button className="primary" type="submit">전송</button>
      </form>
    </div>
  );
}

function Explore({ authed }) {
  const [city, setCity] = useState("Seoul");
  const [weather, setWeather] = useState(null);
  const [symbol, setSymbol] = useState("AAPL");
  const [stock, setStock] = useState(null);
  const [watch, setWatch] = useState([]);
  const [error, setError] = useState("");

  async function loadWatch() {
    if (!authed) return;
    setWatch(await api.watchlist());
  }

  useEffect(() => { loadWatch().catch((err) => setError(err.message)); }, [authed]);

  return (
    <div className="layout">
      <section className="list">
        <form className="panel composer" onSubmit={async (event) => {
          event.preventDefault();
          try { setError(""); setWeather(await api.weather(city)); }
          catch (err) { setError(err.message); }
        }}>
          <h2>날씨</h2>
          <input value={city} onChange={(event) => setCity(event.target.value)} />
          <button className="primary" type="submit">보기</button>
          {weather && <p>{weather.city} {weather.temperature}°C · {weather.description}</p>}
        </form>
        <form className="panel composer" onSubmit={async (event) => {
          event.preventDefault();
          try { setError(""); setStock(await api.stock(symbol)); }
          catch (err) { setError(err.message); }
        }}>
          <h2>시세</h2>
          <input value={symbol} onChange={(event) => setSymbol(event.target.value)} />
          <button className="primary" type="submit">조회</button>
          {stock && <p>{stock.symbol} {stock.name} · {stock.price} {stock.currency}</p>}
        </form>
        <ErrorNote error={error} />
      </section>
      <aside className="panel side">
        <h2>관심종목</h2>
        {!authed && <p className="meta">저장은 로그인 뒤입니다. 조회는 열려 있습니다.</p>}
        {authed && (
          <button className="ghost" onClick={async () => {
            try { await api.addWatch(symbol); await loadWatch(); }
            catch (err) { setError(err.message); }
          }}>현재 심볼 담기</button>
        )}
        {watch.map((item) => (
          <div className="row" key={item.symbol}>
            <span>{item.symbol} {item.price}</span>
            <button className="ghost" onClick={async () => { await api.removeWatch(item.symbol); await loadWatch(); }}>빼기</button>
          </div>
        ))}
      </aside>
    </div>
  );
}

function Desk({ authed }) {
  const [members, setMembers] = useState([]);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [storageKey, setStorageKey] = useState("memo");
  const [storageValue, setStorageValue] = useState("");
  const [items, setItems] = useState([]);
  const [fileInfo, setFileInfo] = useState(null);
  const [stream, setStream] = useState("연결 전");
  const [error, setError] = useState("");

  async function loadMembers() {
    setMembers(await api.members());
  }
  async function loadStorage() {
    setItems(await api.storage());
  }

  useEffect(() => {
    if (!authed) return;
    Promise.all([loadMembers(), loadStorage()]).catch((err) => setError(err.message));
  }, [authed]);

  useEffect(() => {
    if (!authed) return undefined;
    const token = getToken();
    const source = new EventSource(`/notifications/stream?token=${encodeURIComponent(token)}`);
    source.addEventListener("connected", () => setStream("연결됨"));
    source.addEventListener("new-message", (event) => setStream(`새 쪽지 ${event.data}`));
    source.onerror = () => setStream("연결이 끊겼습니다");
    return () => source.close();
  }, [authed]);

  if (!authed) return <section className="panel"><h2>작업실</h2><p>회원, 파일, 저장소, 알림은 로그인 뒤입니다.</p></section>;

  return (
    <div className="layout">
      <section className="list">
        <form className="panel composer" onSubmit={async (event) => {
          event.preventDefault();
          try {
            setError("");
            await api.createMember({ name, email });
            setName("");
            setEmail("");
            await loadMembers();
          } catch (err) { setError(err.message); }
        }}>
          <h2>회원</h2>
          <input value={name} onChange={(event) => setName(event.target.value)} placeholder="이름" required />
          <input value={email} onChange={(event) => setEmail(event.target.value)} placeholder="이메일" required />
          <button className="primary" type="submit">추가</button>
          {members.map((member) => (
            <div className="row" key={member.id}>
              <span>{member.name} · {member.email}</span>
              <button className="ghost" type="button" onClick={async () => { await api.deleteMember(member.id); await loadMembers(); }}>삭제</button>
            </div>
          ))}
        </form>
        <form className="panel composer" onSubmit={async (event) => {
          event.preventDefault();
          const file = event.target.elements.file.files[0];
          if (!file) return;
          try { setFileInfo(await api.upload(file)); }
          catch (err) { setError(err.message); }
        }}>
          <h2>파일</h2>
          <input name="file" type="file" />
          <button className="primary" type="submit">올리기</button>
          {fileInfo && <a href={fileInfo.url}>{fileInfo.originalName}</a>}
        </form>
      </section>
      <section className="list">
        <form className="panel composer" onSubmit={async (event) => {
          event.preventDefault();
          try {
            await api.saveStorage(storageKey, storageValue);
            await loadStorage();
          } catch (err) { setError(err.message); }
        }}>
          <h2>개인 저장소</h2>
          <input value={storageKey} onChange={(event) => setStorageKey(event.target.value)} placeholder="키" required />
          <input value={storageValue} onChange={(event) => setStorageValue(event.target.value)} placeholder="값" required />
          <button className="primary" type="submit">저장</button>
          {items.map((item) => (
            <div className="row" key={item.key}>
              <span>{item.key}: {item.value}</span>
              <button className="ghost" type="button" onClick={async () => { await api.deleteStorage(item.key); await loadStorage(); }}>삭제</button>
            </div>
          ))}
        </form>
        <section className="panel">
          <h2>알림</h2>
          <p>{stream}</p>
          <p className="meta">이 계정으로 쪽지가 오면 new-message 이벤트가 표시됩니다.</p>
        </section>
        <ErrorNote error={error} />
      </section>
    </div>
  );
}

export default function App() {
  const [view, setView] = useState("home");
  const [postId, setPostId] = useState(null);
  const [authed, setAuthed] = useState(Boolean(getToken()));

  function openPost(id) {
    setPostId(id);
    setView("post");
  }

  return (
    <div className="app">
      <header className="top">
        <button className="brand" onClick={() => setView("home")}>
          <strong>결</strong>
          <span>게시판</span>
        </button>
        <nav>
          {NAV.map(([id, label]) => (
            <button key={id} aria-current={view === id ? "page" : undefined} onClick={() => setView(id)}>{label}</button>
          ))}
          <button aria-current={view === "auth" ? "page" : undefined} onClick={() => setView("auth")}>
            {authed ? "계정" : "입장"}
          </button>
        </nav>
      </header>
      {view === "home" && <Home onWrite={() => setView("board")} />}
      {view === "board" && <Board onOpen={openPost} />}
      {view === "post" && <PostView id={postId} onBack={() => setView("board")} />}
      {view === "auth" && <AuthBox onChange={setAuthed} />}
      {view === "messages" && <Messages authed={authed} />}
      {view === "explore" && <Explore authed={authed} />}
      {view === "desk" && <Desk authed={authed} />}
    </div>
  );
}
