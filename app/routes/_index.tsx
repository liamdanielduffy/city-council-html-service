import type { LoaderFunction } from "@remix-run/node";
import { PrismaClient } from "@prisma/client";

export let loader: LoaderFunction = async ({ request }) => {
  const url = new URL(request.url);
  const page = url.searchParams.get("page");

  const prisma = new PrismaClient();
  const scrapedPage = await prisma.scrapedCalendarPage.findUnique({
    where: { pageNumber: Number(page) },
  });
  if (scrapedPage === null) {
    return new Response("Page not found", { status: 404 });
  } else {
    return new Response(scrapedPage.html, {
      headers: { "Content-Type": "text/html" },
    });
  }
};
