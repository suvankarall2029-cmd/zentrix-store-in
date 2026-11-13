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
const ADMIN_UID = "admin_789";

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

function validCompletedProfile(uid, overrides = {}) {
  return {
    userId: uid,
    publicUserId: "ZX-ALICE123",
    displayName: "Alice Roy",
    firstName: "Alice",
    middleName: "",
    lastName: "Roy",
    fullName: "Alice Roy",
    email: "alice@zentrixstore.app",
    phoneNumber: "+919876543210",
    countryCode: "+91",
    age: 22,
    dateOfBirth: "2004-05-10",
    ageGroup: "18-24",
    purpose: "Android App Developer",
    interests: ["Android Applications", "Java / Kotlin"],
    emailVerified: true,
    phoneVerified: false,
    isVerified: true,
    status: "ACTIVE",
    role: "USER",
    registrationCompleted: true,
    authProvider: "GOOGLE",
    language: "en",
    createdAt: new Date(),
    lastLoginAt: new Date(),
    updatedAt: new Date(),
    ...overrides,
  };
}

test("Unauthenticated user: cannot read courses or enrollments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("courses").get());
  await assertFails(unauthDb.collection("enrollments").get());
  await assertFails(unauthDb.collection("users").get());
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

test("Registration: valid completed profile succeeds with USER role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@zentrixstore.app",
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validCompletedProfile(ALICE_UID))
  );
});

test("Security: rejects self-assigned ADMIN role on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@zentrixstore.app",
    email_verified: true,
  }).firestore();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validCompletedProfile(ALICE_UID, { role: "ADMIN" })
    )
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validCompletedProfile(ALICE_UID))
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ role: "ADMIN" })
  );
});

test("Security: rejects plaintext password or passwordHash in Firestore", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validCompletedProfile(ALICE_UID, { password: "Password@123" })
    )
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validCompletedProfile(ALICE_UID, { passwordHash: "deadbeef" })
    )
  );
});

test("Security: rejects underage registration (< 13)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validCompletedProfile(ALICE_UID, { age: 11, ageGroup: "<13" })
    )
  );
});

test("Security: regular user cannot read Bob's profile or list all users", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set(
      validCompletedProfile(BOB_UID, { email: "bob@zentrixstore.app" })
    );
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Admin security: verified bootstrapped admin can list users and suspend/promote; spoofed unverified email fails", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set(
      validCompletedProfile(BOB_UID, { email: "bob@zentrixstore.app" })
    );
  });

  // Spoofed email without email_verified == true must fail
  const spoofedDb = testEnv.authenticatedContext("spoof_uid", {
    email: "suvankarall2029@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofedDb.collection("users").get());
  await assertFails(
    spoofedDb.collection("users").doc(BOB_UID).update({ status: "SUSPENDED" })
  );

  // Verified admin succeeds
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "suvankarall2029@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      status: "SUSPENDED",
      updatedAt: new Date(),
    })
  );
});

test("Duplicate prevention: contactLocks blocks list and prevents overwriting another user's lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("contactLocks").doc("ph_hash_9876543210").set({
      userId: ALICE_UID,
      contactType: "PHONE",
      createdAt: new Date(),
    })
  );

  await assertFails(bobDb.collection("contactLocks").get());
  await assertFails(
    bobDb.collection("contactLocks").doc("ph_hash_9876543210").set({
      userId: BOB_UID,
      contactType: "PHONE",
      createdAt: new Date(),
    })
  );
});
