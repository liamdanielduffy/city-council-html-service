import type { LoaderFunction } from "@remix-run/node";
import { PrismaClient } from "@prisma/client";
import { useLoaderData } from "@remix-run/react";

export let loader: LoaderFunction = async ({ request }) => {
  const prisma = new PrismaClient();
  const res = await prisma.cityCouncilMeeting.findMany({
    orderBy: {
      dateTime: 'desc'
    }
  });
  if (res === null) {
    return new Response("Page not found", { status: 404 });
  } else {
    return new Response(JSON.stringify(res), {
      headers: { "Content-Type": "application/json" },
    });
  }
};

export default function Page() {
  const data = useLoaderData<typeof loader>()
  return (
    <>
      {data.map(d => (
        <>
          <p>{d.name}</p>
          <p>{d.dateTime}</p>
          <p>{d.location}</p>
          {d.topic && <p>{d.topic}</p>}
          {d.detailsUrl && <a target="_blank" href={d.detailsUrl} rel="noreferrer">details</a>}
          {d.agendaUrl && <><br /><a target="_blank" href={d.agendaUrl} rel="noreferrer">agenda</a></>}
          {d.minutesUrl && <><br /><a target="_blank" href={d.minutesUrl} rel="noreferrer">minutes</a></>}
          {d.videoUrl && <><br /><a target="_blank" href={d.videoUrl} rel="noreferrer">video</a></>}
          <br />
          <br />
          <br />
        </>))}
    </>
  )
}
