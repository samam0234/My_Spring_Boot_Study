const TOKEN_KEY = "gyeol.token";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = getToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...options, headers });
  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      if (response.ok) {
        return text;
      }
      data = { message: text };
    }
  }
  if (!response.ok) {
    const error = new Error(data?.message || `요청이 거절되었습니다. (${response.status})`);
    error.status = response.status;
    throw error;
  }
  return data;
}

function query(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const text = search.toString();
  return text ? `?${text}` : "";
}

export const api = {
  hello: () => request("/hello"),
  time: () => request("/time/status"),
  profile: () => request("/time/profile"),
  posts: (params) => request(`/posts${query(params)}`),
  post: (id) => request(`/posts/${id}`),
  createPost: (body) => request("/posts", { method: "POST", body: JSON.stringify(body) }),
  updatePost: (id, body) => request(`/posts/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  deletePost: (id) => request(`/posts/${id}`, { method: "DELETE" }),
  signup: (body) => request("/auth/signup", { method: "POST", body: JSON.stringify(body) }),
  login: (body) => request("/auth/login", { method: "POST", body: JSON.stringify(body) }),
  inbox: () => request("/messages/inbox"),
  sent: () => request("/messages/sent"),
  sendMessage: (body) => request("/messages", { method: "POST", body: JSON.stringify(body) }),
  weather: (city) => request(`/weather${query({ city })}`),
  stock: (symbol) => request(`/stocks/${encodeURIComponent(symbol)}`),
  watchlist: () => request("/watchlist"),
  addWatch: (symbol) => request("/watchlist", { method: "POST", body: JSON.stringify({ symbol }) }),
  removeWatch: (symbol) => request(`/watchlist/${encodeURIComponent(symbol)}`, { method: "DELETE" }),
  members: () => request("/members"),
  createMember: (body) => request("/members", { method: "POST", body: JSON.stringify(body) }),
  deleteMember: (id) => request(`/members/${id}`, { method: "DELETE" }),
  storage: () => request("/storage"),
  saveStorage: (key, value) => request(`/storage/${encodeURIComponent(key)}`, {
    method: "PUT",
    body: JSON.stringify({ value }),
  }),
  deleteStorage: (key) => request(`/storage/${encodeURIComponent(key)}`, { method: "DELETE" }),
  upload: (file) => {
    const body = new FormData();
    body.append("file", file);
    return request("/files", { method: "POST", body });
  },
};
