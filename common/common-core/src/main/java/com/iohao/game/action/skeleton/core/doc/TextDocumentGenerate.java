/*
 * ioGame
 * Copyright (C) 2021 - present  渔民小镇 （262610965@qq.com、luoyizhu@gmail.com） . All Rights Reserved.
 * # iohao.com . 渔民小镇
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.iohao.game.action.skeleton.core.doc;

import com.iohao.game.action.skeleton.core.CmdInfo;
import com.iohao.game.common.kit.StrKit;
import com.iohao.game.common.kit.io.FileKit;
import com.iohao.game.common.kit.time.FormatTimeKit;
import lombok.Setter;

import java.io.File;
import java.util.*;

/**
 * 文本文档生成
 *
 * @author 渔民小镇
 * @date 2024-06-25
 */
public final class TextDocumentGenerate implements DocumentGenerate {
    private final StringJoiner docContentJoiner = new StringJoiner(System.lineSeparator());
    /** 文档生成后所存放的目录 */
    @Setter
    String path = System.getProperty("user.dir") + File.separator + "doc_game.txt";

    @Override
    public void generate(IoGameDocument ioGameDocument) {
        // 加上游戏文档格式说明
        this.gameDocURLDescription();

        Map<Integer, BroadcastDocument> broadcastDocumentMap = new TreeMap<>();
        ioGameDocument.getBroadcastDocumentList().forEach(broadcastDocument -> {
            // map put
            broadcastDocumentMap.put(broadcastDocument.getCmdMerge(), broadcastDocument);
        });

        // 生成文档 - action
        ioGameDocument.getActionDocList().forEach(actionDoc -> {
            var docInfo = new DocInfo();

            actionDoc.stream()
                    .map(ActionCommandDoc::getActionCommand)
                    .filter(Objects::nonNull)
                    .filter(actionCommand -> {
                        var cmdInfo = actionCommand.getCmdInfo();
                        var authentication = IoGameDocumentHelper.getDocumentAccessAuthentication();
                        var cmdMerge = cmdInfo.getCmdMerge();
                        // 路由访问权限控制
                        return !authentication.reject(cmdMerge);
                    }).forEach(subBehavior -> {
                        docInfo.setHead(subBehavior);
                        docInfo.add(subBehavior);
                    });

            if (docInfo.getSubBehaviorList().isEmpty()) {
                return;
            }

            docInfo.broadcastDocumentMap = broadcastDocumentMap;
            String render = docInfo.render();
            this.docContentJoiner.add(render);
        });

        // 生成文档 - 广播（推送）文档
        extractedBroadcastDoc(broadcastDocumentMap);

        // 生成文档 - 错误码文档
        extractedErrorCode(ioGameDocument);

        // 写文件
        String docText = this.docContentJoiner.toString();
        FileKit.writeUtf8String(docText, path);
    }

    private void gameDocURLDescription() {
        // 加上游戏文档格式说明
        String gameDocInfo = """
                ==================== 游戏文档格式说明 ====================
                https://www.yuque.com/iohao/game/irth38#cJLdC
                """;

        this.docContentJoiner.add("generate %s".formatted(FormatTimeKit.format()));
        this.docContentJoiner.add(gameDocInfo);
    }

    private void extractedBroadcastDoc(Map<Integer, BroadcastDocument> broadcastDocumentMap) {

        var broadcastDocumentList = broadcastDocumentMap.values();
        if (broadcastDocumentList.isEmpty()) {
            return;
        }

        this.docContentJoiner.add("==================== 其它广播推送 ====================");

        for (BroadcastDocument broadcastDocument : broadcastDocumentList) {

            String template = "路由: {cmd} - {subCmd}  --- 广播推送: {dataClass} {dataDescription}";

            if (StrKit.isNotEmpty(broadcastDocument.getMethodDescription())) {
                template = "路由: {cmd} - {subCmd}  --- 广播推送: {dataClass} {dataDescription}，({description})";
            }

            var stringObjectMap = new HashMap<>();
            stringObjectMap.put("cmd", broadcastDocument.getCmd());
            stringObjectMap.put("subCmd", broadcastDocument.getSubCmd());
            stringObjectMap.put("dataClass", broadcastDocument.getDataClassName());
            stringObjectMap.put("description", broadcastDocument.getMethodDescription());
            stringObjectMap.put("dataDescription", broadcastDocument.getDataDescription());

            String format = StrKit.format(template, stringObjectMap);
            this.docContentJoiner.add(format);
        }

        this.docContentJoiner.add("");
    }

    private void extractedErrorCode(IoGameDocument ioGameDocument) {

        this.docContentJoiner.add("==================== 错误码 ====================");

        for (ErrorCodeDocument errorCodeDocument : ioGameDocument.getErrorCodeDocumentList()) {
            String format = "%s : %s : %s".formatted(errorCodeDocument.getValue(),
                    errorCodeDocument.getDescription(),
                    errorCodeDocument.getName()
            );

            this.docContentJoiner.add(format);
        }
    }
}
