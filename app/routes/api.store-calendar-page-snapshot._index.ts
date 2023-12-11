import { PrismaClient } from '@prisma/client';
import { json } from '@remix-run/node';
import type { ActionFunction } from '@remix-run/node';

const prisma = new PrismaClient();

export let action: ActionFunction = async ({ request }: { request: Request }) => {
  if (request.method !== 'POST') {
    return json(
      { error: 'This endpoint only accepts POST requests' },
      { status: 405 }
    );
  }

  const body = await request.json();
  const { pageNumber, html } = body;

  if (!pageNumber || !html) {
    return json(
      { error: 'pageNumber and html fields are required' },
      { status: 400 }
    );
  }

  await prisma.calendarPageSnapshot.upsert({
    where: {
      pageNumber
    },
    create: {
      pageNumber,
      html
    },
    update: {
      pageNumber,
      html
    }
  })

  return json({ status: 'success' }, { status: 200 });
};

