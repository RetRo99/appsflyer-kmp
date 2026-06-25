import Foundation
import OSLog

public class PlatformLogsBridge: NSObject {
    private var running = false

    @objc public func readLogs(tag: String, onEntry: @escaping (String, String, Int) -> Void) {
        if #available(iOS 15.0, *) {
            running = true
            DispatchQueue.global(qos: .utility).async { [weak self] in
                guard let self = self else { return }
                guard let store = try? OSLogStore(scope: .currentProcessIdentifier) else { return }
                var lastDate = Date()

                while self.running {
                    do {
                        let pos = store.position(date: lastDate)
                        let entries = try store.getEntries(at: pos)
                        for entry in entries {
                            if !self.running { break }
                            if entry.date <= lastDate { continue }
                            lastDate = entry.date

                            if let logEntry = entry as? OSLogEntryLog {
                                let message = logEntry.composedMessage
                                if tag.isEmpty || message.contains(tag) {
                                    let timestamp = self.formatDate(entry.date)
                                    let level: Int
                                    switch logEntry.level {
                                    case .debug: level = 1
                                    case .info, .notice: level = 2
                                    case .error, .fault: level = 4
                                    default: level = 2
                                    }
                                    onEntry(timestamp, message, level)
                                }
                            }
                        }
                    } catch {
                    }
                    Thread.sleep(forTimeInterval: 0.5)
                }
            }
        }
    }

    @objc public func stop() {
        running = false
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss.SSS"
        return formatter.string(from: date)
    }
}
