"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const client_1 = require("@prisma/client");
const prisma = new client_1.PrismaClient();
function main() {
    return __awaiter(this, void 0, void 0, function* () {
        try {
            const totalLogs = yield prisma.activityLog.count();
            console.log(`Total activity logs: ${totalLogs}`);
            const logs = yield prisma.activityLog.findMany({
                take: 50,
                orderBy: { id: 'desc' },
                include: { employee: true }
            });
            console.log('=== LATEST 50 LOGS ===');
            logs.forEach((log) => {
                var _a, _b;
                console.log(`ID: ${log.id} | Action: ${log.action} | Emp: ${(_a = log.employee) === null || _a === void 0 ? void 0 : _a.name} (Role: ${(_b = log.employee) === null || _b === void 0 ? void 0 : _b.role}) | Desc: ${log.description}`);
            });
        }
        catch (error) {
            console.error(error);
        }
        finally {
            yield prisma.$disconnect();
        }
    });
}
main();
