import { json } from '@remix-run/node';
import type { ActionFunction } from '@remix-run/node';

export let action: ActionFunction = async ({ request }: { request: Request }) => {
  if (request.method !== 'POST') {
    return json(
      { error: 'This endpoint only accepts POST requests' },
      { status: 405 }
    );
  }

  const body = await request.json();
  const { page_number, html } = body;

  if (!page_number || !html) {
    return json(
      { error: 'page_number and html fields are required' },
      { status: 400 }
    );
  }

  console.log(`Page Number: ${page_number}`);
  console.log(`HTML: ${html}`);

  return json({ status: 'success' }, { status: 200 });
};

