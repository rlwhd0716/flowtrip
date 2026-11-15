import { createPlaceProxy, readApiKey } from './server.mjs';
import { once } from 'node:events';

// Real Kakao requests through the same proxy used by the app. Prints no credentials or coordinates.
const apiKey = readApiKey();
const server = createPlaceProxy({ apiKey, upstreamFetch: async (url, options) => {
  const response = await fetch(url, options);
  if (!response.ok) {
    const body = await response.clone().json().catch(() => ({}));
    console.log(JSON.stringify({ upstreamStatus: response.status, errorType: body.errorType,
      code: body.code ?? body.error_code,
      message: String(body.message ?? body.msg ?? '').replaceAll(apiKey, '[redacted]').slice(0, 500) }));
  }
  return response;
} });
server.listen(0, '127.0.0.1'); await once(server, 'listening');
try {
  for (const path of [
    '/v2/local/search/address.json?query=' + encodeURIComponent('서울특별시 성동구'),
    '/v2/local/search/keyword.json?query=' + encodeURIComponent('성수동 카페') + '&category_group_code=CE7&size=3',
  ]) {
    const response = await fetch(`http://127.0.0.1:${server.address().port}${path}`);
    const body = await response.json();
    console.log(JSON.stringify({ endpoint: path.split('?')[0], status: response.status, count: body.documents?.length ?? 0,
      places: body.documents?.map(doc => doc.place_name).filter(Boolean) }));
    if (!response.ok || !body.documents?.length) process.exitCode = 1;
  }
} finally { server.closeAllConnections(); server.close(); }
