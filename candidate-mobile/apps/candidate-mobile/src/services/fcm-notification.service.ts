import type * as NotificationsType from 'expo-notifications';

export interface GateClosingAlertOptions {
  examName: string;
  gateClosingTime: Date | string;
  venueName: string;
  minutesBeforeGateClose?: number;
}

export interface ShiftChangeAlertOptions {
  examName: string;
  oldShift: string;
  newShift: string;
  reason?: string;
  venueName?: string;
}

export const NOTIFICATION_CHANNELS = {
  EXAM_GATE_ALERTS: 'exam-gate-alerts',
  ADMIT_CARD_WALLET: 'admit-card-wallet',
  APPLICATION_DEADLINES: 'application-deadlines',
} as const;

let Notifications: typeof NotificationsType = {} as any;
try {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  Notifications = require('expo-notifications');
} catch {
  // Headless test or dev fallback
}

export class FcmNotificationService {
  private static instance: FcmNotificationService;
  private channelsInitialized = false;

  private constructor() {}

  public static getInstance(): FcmNotificationService {
    if (!FcmNotificationService.instance) {
      FcmNotificationService.instance = new FcmNotificationService();
    }
    return FcmNotificationService.instance;
  }

  /**
   * Configures high-consequence Android Notification Channels for NAG Candidate delivery:
   * 1. exam-gate-alerts (Urgent gate countdown, DND bypass, vibration)
   * 2. admit-card-wallet (Digital Hall Ticket & passkey issuance)
   * 3. application-deadlines (Registration deadlines, form submission alerts)
   */
  public async initializeNotificationChannels(): Promise<void> {
    try {
      if (typeof Notifications.setNotificationChannelAsync !== 'function') {
        this.channelsInitialized = true;
        return;
      }

      await Notifications.setNotificationChannelAsync(
        NOTIFICATION_CHANNELS.EXAM_GATE_ALERTS,
        {
          name: 'Exam Gate Alerts & Live Countdown',
          importance: Notifications.AndroidImportance.HIGH,
          vibrationPattern: [0, 250, 250, 250],
          lightColor: '#DC2626',
          bypassDnd: true,
          showBadge: true,
          description:
            'Critical exam center gate closing warnings, reporting time notifications, and live countdowns.',
        }
      );

      await Notifications.setNotificationChannelAsync(
        NOTIFICATION_CHANNELS.ADMIT_CARD_WALLET,
        {
          name: 'Admit Card & Digital Hall Ticket Wallet',
          importance: Notifications.AndroidImportance.HIGH,
          lightColor: '#2563EB',
          showBadge: true,
          description:
            'Cryptographic hall ticket issuance, QR updates, and offline verification pass alerts.',
        }
      );

      await Notifications.setNotificationChannelAsync(
        NOTIFICATION_CHANNELS.APPLICATION_DEADLINES,
        {
          name: 'Application Deadlines & Results',
          importance: Notifications.AndroidImportance.DEFAULT,
          lightColor: '#059669',
          showBadge: true,
          description:
            'Application form closing alerts, fee payment receipts, and result publications.',
        }
      );

      this.channelsInitialized = true;
    } catch {
      // Graceful fallback for non-Android or mock unit testing environments
      this.channelsInitialized = true;
    }
  }

  /**
   * Schedules a gate-closing countdown alert to prevent candidate lockout at exam centres.
   */
  public async scheduleGateClosingCountdown(
    options: GateClosingAlertOptions
  ): Promise<string> {
    await this.ensureChannels();

    const gateTime =
      typeof options.gateClosingTime === 'string'
        ? new Date(options.gateClosingTime).getTime()
        : options.gateClosingTime.getTime();

    const leadMinutes = options.minutesBeforeGateClose ?? 30;
    const triggerTime = gateTime - leadMinutes * 60 * 1000;
    const now = Date.now();
    const delaySeconds = Math.max(1, Math.round((triggerTime - now) / 1000));

    const notificationId = `gate-alert-${options.examName}-${Date.now()}`;

    if (typeof Notifications.scheduleNotificationAsync === 'function') {
      try {
        return await Notifications.scheduleNotificationAsync({
          identifier: notificationId,
          content: {
            title: `CRITICAL: Gate Closes in ${leadMinutes} Minutes!`,
            subtitle: options.examName,
            body: `Centre gates for ${options.examName} at ${options.venueName} close at ${new Date(gateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}. Entry strictly prohibited after gate closing.`,
            data: {
              type: 'GATE_CLOSING_ALERT',
              examName: options.examName,
              venue: options.venueName,
              closingTime: gateTime,
            },
            sound: true,
          },
          trigger: {
            channelId: NOTIFICATION_CHANNELS.EXAM_GATE_ALERTS,
            seconds: delaySeconds,
          },
        });
      } catch {
        return notificationId;
      }
    }

    return notificationId;
  }

  /**
   * Sends an immediate high-priority shift change notification.
   */
  public async sendShiftChangeAlert(
    options: ShiftChangeAlertOptions
  ): Promise<string> {
    await this.ensureChannels();

    const notificationId = `shift-change-${options.examName}-${Date.now()}`;

    if (typeof Notifications.scheduleNotificationAsync === 'function') {
      try {
        return await Notifications.scheduleNotificationAsync({
          identifier: notificationId,
          content: {
            title: `URGENT: Shift Schedule Updated - ${options.examName}`,
            subtitle: 'Examination Timing Modification',
            body: `Your examination shift has been rescheduled from ${options.oldShift} to ${options.newShift}.${options.reason ? ` Reason: ${options.reason}` : ''}`,
            data: {
              type: 'SHIFT_CHANGE_ALERT',
              examName: options.examName,
              oldShift: options.oldShift,
              newShift: options.newShift,
              reason: options.reason,
            },
            sound: true,
          },
          trigger: {
            channelId: NOTIFICATION_CHANNELS.EXAM_GATE_ALERTS,
            seconds: 1,
          },
        });
      } catch {
        return notificationId;
      }
    }

    return notificationId;
  }

  /**
   * Sends hall ticket issuance alert.
   */
  public async sendAdmitCardIssuedAlert(
    examName: string,
    rollNumber: string
  ): Promise<string> {
    await this.ensureChannels();

    const notificationId = `admit-card-${rollNumber}-${Date.now()}`;

    if (typeof Notifications.scheduleNotificationAsync === 'function') {
      try {
        return await Notifications.scheduleNotificationAsync({
          identifier: notificationId,
          content: {
            title: `Admit Card Issued: ${examName}`,
            subtitle: `Roll No: ${rollNumber}`,
            body: `Your official digital admit card is ready. Download it to your offline secure wallet before exam day.`,
            data: {
              type: 'ADMIT_CARD_ISSUED',
              examName,
              rollNumber,
            },
          },
          trigger: {
            channelId: NOTIFICATION_CHANNELS.ADMIT_CARD_WALLET,
            seconds: 1,
          },
        });
      } catch {
        return notificationId;
      }
    }

    return notificationId;
  }

  /**
   * Cancels a scheduled alert.
   */
  public async cancelAlert(notificationId: string): Promise<void> {
    if (typeof Notifications.cancelScheduledNotificationAsync === 'function') {
      try {
        await Notifications.cancelScheduledNotificationAsync(notificationId);
      } catch {
        // Fallback no-op
      }
    }
  }

  private async ensureChannels(): Promise<void> {
    if (!this.channelsInitialized) {
      await this.initializeNotificationChannels();
    }
  }
}

export const fcmNotificationService = FcmNotificationService.getInstance();
