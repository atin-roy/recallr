import Link from "next/link";
import { Logo } from "@/components/Logo";
import styles from "./page.module.css";

const topicRows = [
  { title: "Cell respiration", meta: "8 notes / 19 cards / 12 MCQs" },
  { title: "Organic mechanisms", meta: "5 notes / 27 cards / 18 MCQs" },
  { title: "World War I causes", meta: "4 notes / 11 cards / 9 MCQs" },
];

const packets = [
  {
    code: "Packet 01",
    label: "Topic packets",
    text: "Keep the note, cards, and practice questions for a topic in one place.",
  },
  {
    code: "Packet 02",
    label: "Review queue",
    text: "See the material that is due before it disappears from memory.",
  },
  {
    code: "Packet 03",
    label: "Question bank",
    text: "Build MCQs beside your notes so testing is part of studying.",
  },
];

const workflow = [
  "Create a topic for the chapter, lecture, or exam unit.",
  "Write notes and attach cards to the exact facts worth recalling.",
  "Add MCQs for concepts that need practice, not rereading.",
  "Review the queue and mark each item remembered or forgotten.",
];

const mvp = [
  "Now: accounts, topics, notes, flashcards, MCQs.",
  "Next: due reviews and remembered/forgotten history.",
  "Later: AI help for turning notes into practice material.",
];

export default function Home() {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <nav className={styles.nav} aria-label="Main navigation">
          <Link href="/" className={styles.brand} aria-label="recallr home">
            <Logo />
          </Link>
          <div className={styles.navLinks}>
            <a className={styles.navLink} href="#system">
              System
            </a>
            <a className={styles.navLink} href="#workflow">
              Workflow
            </a>
            <a className={styles.navLink} href="#roadmap">
              MVP
            </a>
          </div>
          <p className={styles.navStatus}>Desk file / review queue active</p>
          <Link href="/register" className={styles.navCta}>
            Start a topic
          </Link>
        </nav>
      </header>

      <section className={styles.hero}>
        <div className={styles.heroCopy}>
          <p className={styles.eyebrow}>
            Index-card study desk / not a folder dump
          </p>
          <div>
            <h1 className={styles.title}>Build recall, not just folders.</h1>
            <p className={styles.lead}>
              recallr turns a topic into notes, flashcards, and MCQ practice.
              It is for students who forget what they studied last week and need
              a quieter way to test themselves before the exam does.
            </p>
            <div className={styles.actions}>
              <Link href="/register" className={styles.primary}>
                Create study space
              </Link>
              <a href="#preview" className={styles.secondary}>
                View product sketch
              </a>
            </div>
          </div>
          <div className={styles.currentRow}>
            <span className={styles.currentLabel}>Current</span>
            <span>Biology finals / 14 due before Friday</span>
          </div>
        </div>

        <div id="preview" className={styles.preview}>
          <div className={styles.previewHead}>
            <div>
              <span className={styles.previewKicker}>Topic box</span>
              <span className={styles.previewName}>Cell respiration</span>
            </div>
            <span className={styles.previewBadge}>Due today: 14</span>
          </div>

          <div className={styles.previewBody}>
            <aside className={styles.topicList}>
              <p className={styles.topicListHead}>Topics</p>
              <div className={styles.topicItems}>
                {topicRows.map((topic, index) => (
                  <div
                    key={topic.title}
                    className={
                      index === 0
                        ? `${styles.topic} ${styles.topicActive}`
                        : styles.topic
                    }
                  >
                    <div className={styles.topicRow}>
                      <h2 className={styles.topicName}>{topic.title}</h2>
                      <span
                        className={
                          index === 0
                            ? `${styles.topicDot} ${styles.topicDotActive}`
                            : styles.topicDot
                        }
                        aria-hidden="true"
                      />
                    </div>
                    <p className={styles.topicMeta}>{topic.meta}</p>
                  </div>
                ))}
              </div>
            </aside>

            <div className={styles.previewMain}>
              <div className={styles.notePanel}>
                <p className={styles.panelLabel}>Note with attached cards</p>
                <h2 className={styles.noteTitle}>
                  Glycolysis produces ATP before oxygen is used.
                </h2>
                <p className={styles.noteText}>
                  Highlighted facts become cards, and confusing steps can be
                  turned into MCQs from the same topic page.
                </p>
                <div className={styles.cardGrid}>
                  <div className={`${styles.studyCard} ${styles.cardFlash}`}>
                    <p className={styles.studyKind}>Flashcard</p>
                    <p className={styles.studyPrompt}>
                      What molecule enters glycolysis?
                    </p>
                    <p className={styles.studyFoot}>Front side</p>
                  </div>
                  <div className={`${styles.studyCard} ${styles.cardMcq}`}>
                    <p className={styles.studyKind}>MCQ</p>
                    <p className={styles.studyPrompt}>
                      Which step makes the first ATP?
                    </p>
                    <p className={styles.studyFoot}>4 choices saved</p>
                  </div>
                </div>
              </div>

              <div className={styles.queuePanel}>
                <div className={styles.queueMeta}>
                  <p className={styles.panelLabel}>Review queue</p>
                  <h2 className={styles.queueTitle}>
                    Review what you are about to forget.
                  </h2>
                  <div className={styles.queueRow}>
                    <span>Pyruvate yield</span>
                    <span className={styles.queueDue}>due now</span>
                  </div>
                </div>
                <div className={styles.queueActions}>
                  <span className={styles.btnForgot}>Forgot</span>
                  <span className={styles.btnRemember}>Remembered</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="system" className={styles.system}>
        <div className={styles.systemInner}>
          <div className={styles.systemHead}>
            <p className={styles.systemKicker}>Catalog drawer</p>
            <h2 className={styles.systemTitle}>
              One topic. Notes, cards, and questions attached.
            </h2>
          </div>
          <div className={styles.packets}>
            {packets.map((packet) => (
              <article key={packet.label} className={styles.packet}>
                <p className={styles.packetCode}>{packet.code}</p>
                <h3 className={styles.packetLabel}>{packet.label}</h3>
                <p className={styles.packetText}>{packet.text}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section id="workflow" className={styles.workflow}>
        <div className={styles.workflowHead}>
          <p className={styles.wfKicker}>Example study workflow</p>
          <h2 className={styles.wfTitle}>
            A study session should end with something to recall.
          </h2>
          <p className={styles.wfText}>
            recallr treats tests as first-class study material. Notes explain,
            cards prompt, and MCQs catch the difference between recognition and
            real memory.
          </p>
        </div>
        <ol className={styles.steps}>
          {workflow.map((step, index) => (
            <li key={step} className={styles.step}>
              <span className={styles.stepNum}>0{index + 1}</span>
              <span className={styles.stepText}>{step}</span>
            </li>
          ))}
        </ol>
      </section>

      <section className={styles.band}>
        <div className={styles.bandInner}>
          <article className={styles.bandCard}>
            <p className={styles.bandKicker}>Why it exists</p>
            <h2 className={styles.bandTitle}>
              Rereading feels productive until the blank page appears.
            </h2>
            <p className={styles.bandText}>
              Students often collect material in one app and test themselves in
              another. recallr keeps the explanation and the recall prompt
              connected, so revision starts while the topic is still fresh.
            </p>
          </article>
          <article
            id="roadmap"
            className={`${styles.bandCard} ${styles.bandCardCream}`}
          >
            <p className={styles.bandKicker}>MVP scope</p>
            <h2 className={styles.bandTitle}>
              First: manual study tools. Later: assisted generation.
            </h2>
            <ul className={styles.bandList}>
              {mvp.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </article>
        </div>
      </section>

      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <p>recallr / personal memory system</p>
          <p>Notes → Cards → Questions → Review</p>
        </div>
      </footer>
    </div>
  );
}
