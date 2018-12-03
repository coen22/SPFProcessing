first = read.csv("C:\\Users\\mazch\\Desktop\\Shit Hub\\SPFProcessing\\input\\Ground_Truth.csv")
library("forecast")
library("tseries")
periods_ahead = 5
start = 20
holdout = 20
thecols=colnames(first)
target = first[(periods_ahead+1):282,colnames(first)]
row.names(target) <- 1:nrow(target)
data = first[1:(282-periods_ahead),colnames(first)]
data = data[-c(1,2)]
target = target[-c(1,2)]
original=target


target$NGDP = log(target$NGDP)
data$NGDP = log(data$NGDP)
first$NGDP = log(first$NGDP)
target$RGDP = log(target$RGDP)
data$RGDP = log(data$RGDP)
first$RGDP = log(first$RGDP)
target$RCON = log(target$RCON)
data$RCON = log(data$RCON)
first$RCON = log(first$RCON)
target$RINV = log(target$RINV)
data$RINV = log(data$RINV)
first$RINV = log(first$RINV)
target$RINVRES = log(target$RINVRES)
data$RINVRES = log(data$RINVRES)
first$RINVRES = log(first$RINVRES)
target$RGSL = log(target$RGSL)
data$RGSL = log(data$RGSL)
first$RGSL = log(first$RGSL)

res = target[(start+1):nrow(target),]
row.names(res) <- 1:nrow(res)
for (j in (start+1):nrow(target)) {
for (i in 3:length(thecols)) {
  output <- ts(target[thecols[i]])
  thing <- output[1:(j-1)]
  output <- ts(original[thecols[i]])
  test_thing <- output[j]
  input_data <- data[1:(j-1),]
  test_data <- data[j,]
  model <- lm(formula = thing ~ HOUSING + NGDP + PGDP + UNEMP + INDPROD + RGDP + RCON + RINV + RINVRES + RFEDGOV + RGSL + RCBI + CPI + PCON + EMP,data = input_data)
  if (thecols[i] %in% c("NGDP","RGDP","RCON","RINV","RINVRES","RGSL")) {
    error = (exp(predict(model,test_data))-test_thing)**2
  } else {
    error = (predict(model,test_data)-test_thing)**2
  }
  res[j-start,thecols[i]] = error
}}

plot.ts(res$NGDP[(length(res$NGDP)-holdout):length(res$NGDP)],col="green")

s1 = sum(res$NGDP[(length(res$NGDP)-holdout):length(res$NGDP)])
s2 = sum(res$UNEMP[(length(res$UNEMP)-holdout):length(res$UNEMP)])
s3 = sum(res$INDPROD[(length(res$INDPROD)-holdout):length(res$INDPROD)])

print(s1)
print(s2)
print(s3)
