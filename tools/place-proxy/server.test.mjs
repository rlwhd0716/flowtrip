import { test } from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { get } from 'node:http';
import { createPlaceProxy } from './server.mjs';

test('fixed upstream gets the key server-side; unknown paths and parameters never reach Kakao', async t => {
  const calls = [];
  const server = createPlaceProxy({ apiKey: 'test-only-key', upstreamFetch: async (url, options) => {
    calls.push({ url, options });
    return Response.json({ documents: [{ id: '123', place_name: 'fixture' }] });
  } });
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => { server.closeAllConnections(); server.close(); });
  const base = `http://127.0.0.1:${server.address().port}`;
  assert.equal((await (await fetch(`${base}/health`)).json()).service, 'flowtrip-place-proxy');
  assert.equal(calls.length, 0);
  const response = await fetch(`${base}/v2/local/search/keyword.json?query=cafe&x=127&y=37`);
  assert.equal(response.status, 200);
  assert.equal((await response.json()).documents.length, 1);
  assert.equal(calls[0].url.origin, 'https://dapi.kakao.com');
  assert.equal(calls[0].options.headers.Authorization, 'KakaoAK test-only-key');
  assert.equal(await (await fetch(`${base}/secrets.properties`)).status, 404);
  assert.equal(await (await fetch(`${base}/v2/local/search/keyword.json?query=cafe&url=https://example.com`)).status, 400);
  assert.equal(await (await fetch(`${base}/v2/local/search/keyword.json?query=cafe`, { headers: { Origin: 'https://example.com' } })).status, 403);
  assert.equal(calls.length, 1);
  const invalidHostStatus = await new Promise((resolve, reject) => {
    get(`${base}/v2/local/search/keyword.json?query=cafe`, { headers: { Host: 'attacker.example' } }, response => {
      response.resume(); resolve(response.statusCode);
    }).on('error', reject);
  });
  assert.equal(invalidHostStatus, 403);
  assert.equal(calls.length, 1);
});

test('upstream failure never becomes mock data or a successful search', async t => {
  let failNetwork = false;
  let disabled = false;
  const server = createPlaceProxy({ apiKey: 'test-only-key', upstreamFetch: async () => {
    if (failNetwork) throw new Error('secret must not be reflected');
    if (disabled) return Response.json({ message: 'App(FlowTrip) disabled OPEN_MAP_AND_LOCAL service.' }, { status: 403 });
    return Response.json({ error: 'secret must not be reflected' }, { status: 401 });
  } });
  server.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(() => { server.closeAllConnections(); server.close(); });
  const url = `http://127.0.0.1:${server.address().port}/v2/local/search/keyword.json?query=cafe`;
  const rejected = await fetch(url);
  assert.equal(rejected.status, 401);
  assert.equal((await rejected.text()).includes('secret'), false);
  disabled = true;
  const forbidden = await fetch(url);
  assert.equal(forbidden.status, 403);
  assert.equal((await forbidden.json()).code, 'KAKAO_SERVICE_DISABLED');
  failNetwork = true;
  const offline = await fetch(url);
  assert.equal(offline.status, 502);
  assert.equal((await offline.text()).includes('secret'), false);
});
