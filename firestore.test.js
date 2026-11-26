const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "Hacker",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile: owner create, partial update, shadow field & immutable rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      username: "Alice",
      appSkinThemeId: "slate",
      subscribed: true,
      userTokens: 999999.0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update on mutable preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      appSkinThemeId: "amoled",
      customSystemInstruction: "Be concise",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost/shadow field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's profile or subcollections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = new Date(Date.now() - 10000);
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      username: "Alice",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1").set({
      userId: ALICE_UID,
      title: "Private Chat",
      modelValue: "gpt-5",
      clientTimestamp: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_2").set({
      userId: BOB_UID,
      title: "Hijack",
      modelValue: "gpt-5",
      clientTimestamp: 1700000000000,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Sessions and messages: Kotlin-mirrored create, update, query, and delete succeed; @DocumentId 'id' rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_abc-123").set({
      userId: ALICE_UID,
      title: "New Chat",
      modelValue: "gpt-5-2025-08-07",
      clientTimestamp: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject redundant 'id' field in payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_bad").set({
      id: "sess_bad",
      userId: ALICE_UID,
      title: "Bad Chat",
      modelValue: "gpt-5",
      clientTimestamp: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      sessionId: "sess_abc-123",
      role: "USER",
      text: "Hello Prof Eddie",
      bookmarked: false,
      clientTimestamp: 1700000001000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").update({
      bookmarked: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Filtered query matching Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").where("sessionId", "==", "sess_abc-123").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").delete()
  );
});

test("Media, memories, token usages & interior designs: valid creates & deletes succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("generated_images").doc("img_1").set({
      userId: ALICE_UID,
      prompt: "Sunset mountain",
      imageUrl: "https://example.com/a.png",
      style: "Cinematic",
      aspectRatio: "16:9",
      model: "gpt-image-1.5",
      clientTimestamp: 1700000000000,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("generated_videos").doc("vid_1").set({
      userId: ALICE_UID,
      prompt: "Ocean waves",
      videoUrl: "https://example.com/v.mp4",
      style: "Realistic",
      aspectRatio: "16:9",
      resolution: "1080p",
      duration: "5s",
      model: "veo",
      clientTimestamp: 1700000000000,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("generated_music").doc("mus_1").set({
      userId: ALICE_UID,
      prompt: "Lo-fi beat",
      musicUrl: "https://example.com/m.mp3",
      genre: "Lo-Fi",
      duration: "30s",
      tempo: "90 BPM",
      clientTimestamp: 1700000000000,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      key: "PREFERENCE",
      content: "Prefers Indonesian explanations",
      clientTimestamp: 1700000000000,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("token_usages").doc("tok_1").set({
      userId: ALICE_UID,
      featureType: "Text Generation",
      detail: "Chat GPT",
      tokenCost: 1.5,
      clientTimestamp: 1700000000000,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("interior_designs").doc("int_1").set({
      userId: ALICE_UID,
      title: "Minimalist Living Room",
      selectedStyle: "Scandinavian",
      reimaginedImageUrl: "https://example.com/room.jpg",
      chatHistoryJson: "[]",
      shoppableItemsJson: "[]",
      clientTimestamp: 1700000000000,
      createdAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("generated_videos").doc("vid_1").delete());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("generated_images").doc("img_1").delete());
});
