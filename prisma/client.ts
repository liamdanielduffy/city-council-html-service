import { PrismaClient } from "@prisma/client";

let client: PrismaClient | undefined

function initPrismaClient() {
  if (!client) {
    client = new PrismaClient()
  }
  return client
}

export const prisma = initPrismaClient()