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

test("Unauthenticated user: cannot read courses or enrollments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("courses").get());
  await assertFails(unauthDb.collection("enrollments").get());
});

test("Authenticated user: can read published courses", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("courses").doc("course_1").set({
      title: "Android Kotlin Masterclass",
      instructor: "Zentrix Team",
      category: "development",
      isFree: true,
      published: true,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("courses").doc("course_1").get());
});

test("Authenticated user: cannot read unpublished courses", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("courses").doc("course_draft").set({
      title: "Draft Course",
      instructor: "Zentrix Team",
      category: "development",
      isFree: true,
      published: false,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("courses").doc("course_draft").get());
});

test("Authenticated user: cannot read Bob's enrollment", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("enrollments").doc("bob_enroll").set({
      userId: BOB_UID,
      courseId: "course_1",
      courseTitle: "Android Masterclass",
      enrolledAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("enrollments").doc("bob_enroll").get());
});

test("Authenticated user: can read own enrollment with query filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("enrollments").doc("alice_enroll").set({
      userId: ALICE_UID,
      courseId: "course_1",
      courseTitle: "Android Masterclass",
      enrolledAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("enrollments").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails enrollment query without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("enrollments").get());
});
