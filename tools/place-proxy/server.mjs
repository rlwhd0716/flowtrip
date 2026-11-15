import http from 'node:http';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const endpoints = new Set(['/v2/local/search/address.json', '/v2/local/search/keyword.json']);
const parameters = new Set(['query', 'x', 'y', 'radius', 'size', 'sort', 'category_group_code', 'page']);

export function readApiKey() {
  const key = process.env.KAKAO_REST_API_KEY || readFileSync(new URL('../../secrets.properties', import.meta.url), 'utf8')
    .match(/^KAKAO_REST_API_KEY\s*=\s*([^\r\n]+)/m)?.[1]?.trim();
  if (!key || !/^[a-fA-F0-9]{32}$/.test(key)) throw new Error('Set KAKAO_REST_API_KEY in environment or root secrets.properties');
  return key;
}

/** Development proxy: loopback only, fixed Kakao upstream, no key/query logging. */
export function createPlaceProxy({ apiKey, upstreamFetch = fetch } = {}) {
  if (!apiKey) throw new Error('Kakao REST API key is required');
  return http.createServer(async (req, res) => {
    const send = (status, body) => {
      res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store', 'x-content-type-options': 'nosniff' });
      res.end(JSON.stringify(body));
    };
    const url = new URL(req.url, 'http://127.0.0.1');
    if (!/^(localhost|127\.0\.0\.1|10\.0\.2\.2)(:\d+)?$/.test(req.headers.host || ''))
      return send(403, { message: 'Local proxy host required' });
    if (req.method !== 'GET') return send(405, { message: 'GET only' });
    if (url.pathname === '/health') return send(200, { service: 'flowtrip-place-proxy', status: 'ready' });
    if (!endpoints.has(url.pathname)) return send(404, { message: 'Unknown endpoint' });
    // Browsers cannot use this local server as a cross-origin credential proxy.
    if (req.headers.origin) return send(403, { message: 'Browser origins are not allowed' });
    if (!url.searchParams.get('query')?.trim() || url.searchParams.get('query').length > 200)
      return send(400, { message: 'query is required (maximum 200 characters)' });
    if ([...url.searchParams.keys()].some(name => !parameters.has(name)) || url.search.length > 2000)
      return send(400, { message: 'Unsupported parameters' });
    const upstream = new URL(url.pathname, 'https://dapi.kakao.com');
    upstream.search = url.search;
    try {
      const response = await upstreamFetch(upstream, {
        headers: { Authorization: `KakaoAK ${apiKey}` },
        redirect: 'error', signal: AbortSignal.timeout(7000),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const disabled = response.status === 403 && String(error.message ?? error.msg ?? '').includes('OPEN_MAP_AND_LOCAL');
        return send(response.status, { code: disabled ? 'KAKAO_SERVICE_DISABLED' : 'KAKAO_REQUEST_REJECTED',
          message: disabled ? 'Enable Kakao Map service for this app' : 'Kakao API rejected the request', upstreamStatus: response.status });
      }
      const body = await response.json();
      if (!Array.isArray(body.documents)) return send(502, { message: 'Invalid Kakao search response' });
      return send(200, body);
    } catch {
      return send(502, { message: 'Kakao search is unavailable or timed out' });
    }
  });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  const port = Number(process.env.FLOWTRIP_PROXY_PORT || 8787);
  if (!Number.isInteger(port) || port < 1024 || port > 65535) throw new Error('Invalid FLOWTRIP_PROXY_PORT');
  const server = createPlaceProxy({ apiKey: readApiKey() });
  server.on('error', () => { process.stderr.write('Place proxy could not start; check the port.\n'); process.exitCode = 1; });
  server.listen(port, '127.0.0.1', () => process.stdout.write(`FlowTrip local place proxy: http://127.0.0.1:${port}\n`));
}
