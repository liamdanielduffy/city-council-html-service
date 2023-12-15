import { prisma } from 'prisma/client';
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

  await prisma.cityCouncilMeeting.upsert({
    where: {
      id: body.id
    },
    create: body,
    update: body
  })

  return json({ status: 'success' }, { status: 200 });
};

