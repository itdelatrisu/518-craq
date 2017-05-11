library(ggplot2)

fig4 <- read.csv("Fig4_read.csv", header = TRUE)
ggplot(fig4, aes(x = Number.of.Clients, y = Average, colour = X)) +
  geom_line() + 
  geom_errorbar(aes(ymin = X1st, ymax = X99th)) + 
  ylab("Reads/s") +
  xlab("Number of Clients") +
  theme(legend.position = c(1, 0), legend.justification = c("right", "bottom"),
        legend.title = element_blank(), legend.background = element_rect(fill = alpha("white", 0)))
ggsave("Fig4_read.png")

# f <- read.csv("~/Downloads/throughput.csv", header = TRUE)
# 
# ggplot(f[f$type == "Read",], aes(x = size, y = throughput, colour = size)) +
#   geom_line() + 
#   geom_errorbar(aes(ymin = X1st, ymax = X99th)) + 
#   ylab("operations/s") +
#   xlab("number of chain nodes") +
#   theme(legend.position = c(1, 0), legend.justification = c("right", "bottom"),
#         legend.title = element_blank(), legend.background = element_rect(fill = alpha("white", 0)))
# ggsave("read_throughput.png")
# 
# ggplot(f[f$type == "Write",], aes(x = size, y = throughput, colour = size)) +
#   geom_line() + 
#   geom_errorbar(aes(ymin = X1st, ymax = X99th)) + 
#   ggtitle("Write throughput as the number of chain nodes increase.") +
#   ylab("operations/s") +
#   xlab("number of chain nodes")
# ggsave("write_throughput.png")
# 
# ggplot(f[f$type == "Test & Set",], aes(x = size, y = throughput, colour = size)) +
#   geom_line() + 
#   geom_errorbar(aes(ymin = X1st, ymax = X99th)) + 
#   ggtitle("Test & Set throughput as the number of chain nodes increase.") +
#   ylab("operations/s") +
#   xlab("number of chain nodes")
# ggsave("tas_throughput.png")

fig6 <- read.csv("Fig6_read_as_write_500.csv", header = TRUE)
ggplot(fig6[fig6$mode != "", ], aes(x = Writes.s, y = Reads.s, colour = mode)) +
  geom_line() + 
  ylab("Reads/s") +
  xlab("Writes/s") +
  theme(legend.position = c(1, 1), legend.justification = c("right", "top"),
        legend.title = element_blank(), legend.background = element_rect(fill = alpha("white", 0)))
ggsave("Fig6_read_as_write_500.png")

fig7 <- read.csv("Fig7_read_as_write_5K.csv", header = TRUE)
ggplot(fig7[fig7$mode != "", ], aes(x = Writes.s, y = Reads.s, colour = mode)) +
  geom_line() + 
  ylab("Reads/s") +
  xlab("Writes/s") +
  theme(legend.position = c(1, 1), legend.justification = c("right", "top"),
        legend.title = element_blank(), legend.background = element_rect(fill = alpha("white", 0)))
ggsave("Fig7_read_as_write_5000.png")

fig8 <- read.csv("Fig8_clean_dirty.csv", header = TRUE)
ggplot(fig8, aes(x = Writes.s, y = Clean.Dirty, colour = type)) +
  geom_line() + 
  ylab("Reads/s") +
  xlab("Writes/s") +
  theme(legend.position = c(1, 1), legend.justification = c("right", "top"),
        legend.title = element_blank(), legend.background = element_rect(fill = alpha("white", 0)))
ggsave("Fig8_clean_dirty.png")
