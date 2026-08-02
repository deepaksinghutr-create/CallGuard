# CallGuard

**CallGuard** is a dual-SIM call management app for Android that gives you independent, granular control over how incoming calls are handled on each SIM card in your phone.

## What is CallGuard?

Most Android phones support two SIM cards, but the operating system treats incoming calls the same way regardless of which SIM they arrive on. CallGuard fixes this by letting you set a **separate call-filtering rule for each SIM slot**, so you can, for example, keep one number open to everyone while restricting the other to only your saved contacts.

This is especially useful for people who:
- Use one SIM for personal contacts and another for business, deliveries, or public-facing numbers
- Want to stop spam and unknown-number calls on one line without affecting the other
- Need different call policies for a work SIM vs. a personal SIM

## What does it do?

For **each SIM** (SIM 1 and SIM 2), you can independently choose one of three rules:

1. **Allow all calls** — no filtering, every call rings through as normal
2. **Allow contacts only** — only numbers saved in your phone's Contacts are allowed to ring; all other calls are silently blocked
3. **Block all calls** — every incoming call on that SIM is blocked

There is also a **fallback rule** that applies on devices where the phone's hardware/OS cannot reliably report which SIM a call came in on — this ensures the app still behaves predictably even on such devices.

## How it works

CallGuard registers itself as Android's **Call Screening app** (an official Android system role) and uses a background monitoring service to detect which SIM slot each incoming call is ringing on. Based on that, and your configured rule for that SIM, it either lets the call through normally or rejects it before it rings.

Blocked calls still appear in your call log as missed calls — the caller simply experiences it as an unreachable/busy number, which is standard behavior for call-screening apps.

## Interface

The app has a simple three-tab layout inspired by modern mobile design:
- **SIM 1** — shows the SIM's number (if available) and its call rule
- **SIM 2** — same, for the second SIM
- **Settings** — permission management, the fallback rule, and a diagnostic log showing recent screened calls for troubleshooting

## Privacy

CallGuard works entirely on-device. It does not send call logs, contacts, or phone numbers to any server — all filtering decisions and logs are stored locally on your phone using Android's private app storage.

## Important limitations

- CallGuard only **rejects incoming calls** — it does not affect outgoing calls
- Reliable per-SIM detection depends on the phone manufacturer and Android version; some devices (especially certain Samsung, Xiaomi, or other heavily customized Android builds) may not always report the SIM slot correctly, in which case the fallback rule applies
- This is currently a personal-use build intended for direct installation, not yet published on the Play Store
