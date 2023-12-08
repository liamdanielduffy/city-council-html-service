import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function createSnapshotWithPages() {
  const data = await Deno.readTextFile('data.json');
  const htmlPages = JSON.parse(data);

  const result = await prisma.$transaction(async (prisma) => {
    const snapshot = await prisma.snapshot.create({
      data: {
        calendarPages: {
          create: htmlPages.map((html, index) => ({
            html: html,
            pageNumber: index + 1,
          })),
        },
        numPages: htmlPages.length,
      },
      include: {
        calendarPages: true,
      },
    });

    return snapshot;
  });

  console.log('Created snapshot with pages:', result);
}

createSnapshotWithPages().catch((e) => {
  console.error(e);
  prisma.$disconnect();
});