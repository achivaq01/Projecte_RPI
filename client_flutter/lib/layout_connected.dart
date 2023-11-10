import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import 'app_data.dart';
import 'widget_selectable_list.dart';

class LayoutConnected extends StatefulWidget {
  const LayoutConnected({Key? key}) : super(key: key);

  @override
  State<LayoutConnected> createState() => _LayoutConnectedState();
}

class _LayoutConnectedState extends State<LayoutConnected> {
  final ScrollController _scrollController = ScrollController();
  final _messageController = TextEditingController();
  final FocusNode _messageFocusNode = FocusNode();

  @override
  Widget build(BuildContext context) {
    AppData appData = Provider.of<AppData>(context);

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOut,
      );
    });

    return CupertinoPageScaffold(
        navigationBar: const CupertinoNavigationBar(
          middle: Text("WebSockets Client"),
        ),
        child: Column(
          children: [
            const SizedBox(height: 52),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(width: 8),
                const Text(
                  "Connected to ",
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w200),
                ),
                const SizedBox(width: 8),
                Text(
                  "ws://${appData.ip}:${appData.port}",
                  style: const TextStyle(
                      fontSize: 16, fontWeight: FontWeight.w400),
                ),
                const Text(
                  ", with ID: ",
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w200),
                ),
                Text(
                  "${appData.mySocketId}",
                  style: const TextStyle(
                      fontSize: 16, fontWeight: FontWeight.w400),
                ),
                Expanded(child: Container()),
                SizedBox(
                  width: 140,
                  height: 32,
                  child: CupertinoButton(
                    onPressed: () {
                      appData.disconnectFromServer();
                    },
                    padding: EdgeInsets.zero,
                    child: const Text(
                      "Disconnect",
                      style: TextStyle(fontSize: 14),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
              ],
            ),
            const SizedBox(height: 8),
            Expanded(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(width: 8),
                  Expanded(
                      flex: 2,
                      child: MediaQuery.removePadding(
                          context: context,
                          removeTop: true,
                          child: ListView.builder(
                              controller: _scrollController,
                              primary: false,
                              padding: EdgeInsets.only(
                                  top: -MediaQuery.of(context).padding.top),
                              itemCount: 1,
                              itemBuilder: (BuildContext context, int index) {
                                return Text(
                                  appData.messages,
                                  style: const TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w200),
                                );
                              }))),
                  const SizedBox(width: 8),
                  Container(
                      color: const Color.fromRGBO(240, 240, 240, 1),
                      width: 142,
                      child: MediaQuery.removePadding(
                          context: context,
                          removeTop: true,
                          child: WidgetSelectableList())),
                  const SizedBox(width: 8),
                ],
              ),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(width: 8),
                const Text(
                  "Message: ",
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w200),
                ),
                Expanded(
                  child: CupertinoTextField(
                    controller: _messageController,
                    focusNode: _messageFocusNode,
                    onSubmitted: (value) {
                      appData.send(_messageController.text);
                      _messageController.text = "";
                      FocusScope.of(context).requestFocus(_messageFocusNode);
                    },
                  ),
                ),
                const SizedBox(width: 8),
                SizedBox(
                  width: 140,
                  height: 32,
                  child: CupertinoButton.filled(
                    onPressed: () {
                      appData.send(_messageController.text);
                      _messageController.text = "";
                      FocusScope.of(context).requestFocus(_messageFocusNode);
                    },
                    padding: EdgeInsets.zero,
                    child: Text(
                      appData.selectedClientIndex == null
                          ? "Broadcast"
                          : "Send",
                      style: const TextStyle(fontSize: 14),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
              ],
            ),
            const SizedBox(height: 8),
          ],
        ));
  }
}
